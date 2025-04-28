/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.executor.service;

import static org.qubership.automation.itf.core.util.converter.IdConverter.toBigInt;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.constants.CacheNames;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.services.projectsettings.AbstractProjectSettingsService;
import org.springframework.beans.factory.annotation.Value;

import com.hazelcast.core.EntryView;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ProjectSettingsService extends AbstractProjectSettingsService {

    private final HazelcastInstance hazelcastClient;
    private final CoreObjectManager coreObjectManager;

    @Value("${hazelcast.project-settings.cache.refill.time.seconds}")
    private long projectSettingsCacheRefillTime;

    /**
     * Get project settings property value.
     * Method will try to get value from Hazelcast NearCache "ATP_ITF_PROJECT_SETTINGS" if remote Hazelcast
     * service\NearCache is not available it will try to get project settings from database and try to fill it to
     * Hazelcast cache. If key is not found in database it will return null.
     *
     * @param projectId - projectId
     * @param shortName - property short name
     * @return trimmed property value or null.
     */
    public String get(Object projectId, String shortName) {
        String value = get(toBigInt(projectId), shortName, getProjectSettingsCache());
        return StringUtils.isNotEmpty(value) ? value.trim() : null;
    }

    @Override
    protected String get(BigInteger projectId, String shortName,
                         IMap<String, Map<String, String>> projectSettingsCache) {
        try {
            Map<String, String> projectSettings = getProjectSettings(projectSettingsCache, projectId.toString());
            if (Objects.nonNull(projectSettings)) {
                return projectSettings.get(shortName);
            }
            log.warn("Something went wrong with Hazelcast project settings cache - there is no project "
                    + "settings for projectId {}. Trying to get them from database...", projectId);
            return getFromDataBaseAndTryToFillCache(projectId, shortName, projectSettingsCache);
        } catch (Exception e) {
            log.error("Error while getting project setting '{}' or filling it to cache for project: {}", shortName,
                    projectId, e);
            return null;
        }
    }

    /**
     * Get project settings property value as boolean.
     * Method will try to get value from Hazelcast NearCache "ATP_ITF_PROJECT_SETTINGS" if remote Hazelcast
     * service\NearCache is not available it will try to get project settings from database and try to fill it to
     * Hazelcast cache. If key is not found in database it will return false.
     *
     * @param projectId - projectId
     * @param shortName - property short name
     * @return trimmed property value as boolean or false.
     */
    public boolean getBoolean(Object projectId, String shortName) {
        String value = get(toBigInt(projectId), shortName, getProjectSettingsCache());
        return StringUtils.isNotEmpty(value) && Boolean.parseBoolean(value.trim());
    }

    /**
     * Get project settings property as INTEGER value.
     * Method will try to get value from Hazelcast NearCache "ATP_ITF_PROJECT_SETTINGS" if remote Hazelcast
     * service\NearCache is not available or key\map is not found it will return null.
     *
     * @param projectId - projectId
     * @param shortName - property short name
     * @return property value as INTEGER or null if short name is not found in cache\db.
     */
    @SuppressWarnings("unused")
    public Integer getInt(Object projectId, String shortName) {
        IMap<String, Map<String, String>> projectSettingsCache = getProjectSettingsCache();
        String value = get(toBigInt(projectId), shortName, projectSettingsCache);
        return StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : null;
    }

    /**
     * Get all project settings by projectId.
     * First, this method will try to get data from Hazelcast NearCache. If remote Hazelcast
     * service\NearCache is not available it will return data from database. Also, after get data from db it will try
     * to fill project settings cache for project.
     *
     * @param projectId projectId
     * @return all project settings by projectId
     */
    @Override
    public Map<String, String> getAll(Object projectId) {
        IMap<String, Map<String, String>> projectsSettingsCache = getProjectSettingsCache();
        Map<String, String> projectSettings = getProjectSettings(projectsSettingsCache, projectId.toString());
        if (Objects.nonNull(projectSettings)) {
            return projectSettings;
        }
        log.warn("Can't get all project settings from cache for project '{}'; getting from db...", projectId);
        return getFromDataBaseAndTryToFillCache(toBigInt(projectId), projectsSettingsCache);
    }

    /**
     * This method using at startup of atp-itf-executor to init ProjectSettings Hazelcast cache.
     * It gets all project settings from database and fills them to
     * REMOTE Hazelcast ATP_ITF_PROJECT_SETTINGS map.
     * <p/>
     * <b>Warning:</b>
     * SAVING TO DATABASE DOESN'T OCCUR!
     * <p/>
     * <b>PLEASE NOTE:</b>
     * Hazelcast Near Cache will be filled by Hazelcast engine after the first 'get(key)' call and if it exists in
     * remote Hazelcast map.
     *
     * @param itfProject ITF project (object)
     */
    public void initCache(StubProject itfProject) {
        BigInteger projectId = (BigInteger) itfProject.getID();
        String pid = projectId.toString();
        IMap<String, Map<String, String>> projectSettingsCache = getProjectSettingsCache();
        if (projectSettingsCache.isLocked(pid)) {
            log.info("Project settings for pid {} are locked by another thread/pod of service, waiting for unlock...",
                    pid);
        }
        projectSettingsCache.lock(pid);
        log.debug("Project settings for projectId {} are locked by current thread/pod", pid);
        try {
            boolean notEmptyAndUpdatedRecently = isNotEmptyAndUpdatedRecently(projectId, projectSettingsCache);
            if (notEmptyAndUpdatedRecently) {
                return;
            }
            Map<String, String> projectSettingsFromDb = getAllFromDataBase(projectId);
            setAll(projectSettingsFromDb, projectSettingsCache, projectId);
        } catch (Exception e) {
            log.error("Error while filling project settings in cache for project: {}", pid, e);
        } finally {
            projectSettingsCache.unlock(pid);
            log.debug("Project settings for projectId {} are unlocked by current thread/pod", pid);
        }
    }

    /**
     * This method fills project settings to Hazelcast ATP_ITF_PROJECT_SETTINGS map only.
     * SAVING TO DATABASE DOESN'T OCCUR!
     *
     * @param project         ITF project (object).
     * @param projectSettings project settings that you want to fill to cache.
     */
    public void fillCache(StubProject project, Map<String, String> projectSettings) {
        IMap<String, Map<String, String>> projectSettingsCache = getProjectSettingsCache();
        setAll(projectSettings, projectSettingsCache, toBigInt(project.getID()));
    }

    /**
     * Update project setting WITH SAVING TO DATABASE.
     *
     * @param projectId - projectId
     * @param shortName - property short name
     * @param value     - property value
     */
    public void update(Object projectId, String shortName, String value, boolean saveToDatabase) {
        if (saveToDatabase) {
            setWithSave(projectId, shortName, value);
            return;
        }
        setWithoutSave(projectId, shortName, value);
    }

    /**
     * Add CUSTOM project setting for project WITH SAVING TO DATABASE.
     * This method used in #set_config velocity directive in monolith.
     *
     * @param projectId - projectId
     * @param shortName - property short name
     * @param value     - property value
     */
    @Deprecated
    public void add(Object projectId, String shortName, String value) {
        setWithSave(projectId, shortName, value);
    }

    @Override
    protected IMap<String, Map<String, String>> getProjectSettingsCache() {
        return hazelcastClient.getMap(CacheNames.ATP_ITF_PROJECT_SETTINGS);
    }

    private void setWithSave(Object projectId, String shortName, String value) {
        try {
            updateDataBase(toBigInt(projectId), shortName, value);
            set(projectId.toString(), shortName, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Error while upsert project setting property to cache or database: '%s' for project"
                            + " %s", shortName, projectId), e);
        }
    }

    private void setWithoutSave(Object projectId, String shortName, String value) {
        try {
            set(projectId.toString(), shortName, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Error while upsert project setting property to cache: '%s' for project %s",
                            shortName, projectId), e);
        }
    }

    private String getFromDataBaseAndTryToFillCache(BigInteger projectId, String shortName, IMap<String,
            Map<String, String>> projectSettingsCacheMap) {
        Map<String, String> projectSettingsFromDb = getAllFromDataBase(projectId);
        setAll(projectSettingsFromDb, projectSettingsCacheMap, projectId);
        return projectSettingsFromDb.get(shortName);
    }

    private Map<String, String> getFromDataBaseAndTryToFillCache(BigInteger projectId, IMap<String,
            Map<String, String>> projectSettingsCacheMap) {
        Map<String, String> projectSettingsFromDb = getAllFromDataBase(projectId);
        setAll(projectSettingsFromDb, projectSettingsCacheMap, projectId);
        return projectSettingsFromDb;
    }

    private void updateDataBase(BigInteger projectId, String shortName, String value) {
        //noinspection unchecked
        coreObjectManager.getSpecialManager(StubProject.class, SearchManager.class)
                .updateProjectSetting(projectId, shortName, value);
    }

    private boolean isNotEmptyAndUpdatedRecently(BigInteger projectId,
                                                 IMap<String, Map<String, String>> projectSettingsCache) {
        EntryView<String, Map<String, String>> entryView = projectSettingsCache.getEntryView(projectId.toString());
        return entryView != null && entryView.getLastUpdateTime()
                > System.currentTimeMillis() - projectSettingsCacheRefillTime * 1000;
    }

    private Map<String, String> getAllFromDataBase(BigInteger projectId) {
        try {
            //noinspection unchecked
            return (Map<String, String>) coreObjectManager.getSpecialManager(StubProject.class, SearchManager.class)
                    .getAllProjectSettingsByProjectId(projectId);
        } catch (Exception e) {
            throw new RuntimeException("Error getting project settings from db for projectId " + projectId, e);
        }
    }

    /**
     * This is internal private service method to fill project settings property to Hazelcast Near Cache only
     * (without saving to database).
     * This method uses the IMap.set() method to avoid returning a new value.
     * IMap.put() method returns a new value, but we don't need it.
     *
     * @param projectId - projectId
     * @param shortName - property short name
     * @param value     - property value
     */
    private void set(String projectId, String shortName, String value) {
        try {
            IMap<String, Map<String, String>> projectSettingsCache = getProjectSettingsCache();
            Map<String, String> projectSettings = getProjectSettings(projectSettingsCache, projectId);
            projectSettings.put(shortName, value);
            projectSettingsCache.set(projectId, projectSettings);
        } catch (Exception e) {
            log.error("Error while set project settings to cache for projectId {}", projectId, e);
        }
    }

    private void setAll(Map<String, String> projectSettings,
                        IMap<String, Map<String, String>> to, BigInteger projectId) {
        try {
            to.set(projectId.toString(), projectSettings);
        } catch (Exception e) {
            log.error("Can't set project settings to Hazelcast cache...", e);
        }
    }
}
