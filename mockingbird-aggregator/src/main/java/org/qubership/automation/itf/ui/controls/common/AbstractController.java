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

package org.qubership.automation.itf.ui.controls.common;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.exceptions.operation.NotActualVersionException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIECIObject;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.util.UIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;

@Getter
public abstract class AbstractController<T extends UIObject, U extends Storable>
        extends AbstractStorableController<U> implements EntityController<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractController.class);

    private boolean isSimple = false;

    @Value("${atp.multi-tenancy.enabled}")
    private Boolean isMultiTenant;

    /**
     * returns all objects of the generic type in repository.
     *
     * @return a list of generic objects
     */
    @Override
    public List<? extends UIObject> getAll() {
        return asListUIObject(isMultiTenant ? getAllPerCluster() : manager().getAll(), false, isSimple);
    }

    private Collection<? extends U> getAllPerCluster() {
        Collection<U> allTenantObjectsList = new ArrayList<>();
        for (String tenantId : TenantContext.getTenantIds(true)) {
            TenantContext.setTenantInfo(tenantId);
            Collection<? extends U> allObjectsList = manager().getAll();
            allTenantObjectsList.addAll(allObjectsList);
        }
        TenantContext.setDefaultTenantInfo();
        allTenantObjectsList.addAll(manager().getAll());
        return allTenantObjectsList;
    }

    public List<UIECIObject> getAllWithEciParams() {
        List<UIECIObject> uiEciObjects = new ArrayList<>();
        Collection<? extends U> collection = isMultiTenant ? getAllPerCluster() : manager().getAll();
        for (U object : collection) {
            uiEciObjects.add(new UIECIObject(object));
        }
        return uiEciObjects;
    }

    /**
     * returns all objects of the generic type and parent in repository.
     * if parent is null returns all.
     *
     * @param parentId parent identifier
     * @return a list of generic objects
     */
    @Override
    public List<? extends UIObject> getAll(String parentId) {
        if (Objects.nonNull(parentId)) {
            return asListUIObject(manager().getAllByParentId(parentId), false, isSimple);
        }
        return getAll();
    }

    /**
     * returns the required object from the repository by the identifier.
     *
     * @param id Storable object in repository
     * @return {@code <T extends UIObject>} by id
     */
    @Override
    public T getById(String id) {
        U object = ControllerHelper.get(id, _getGenericUClass());
        return _newInstanceTClass(object);
    }

    /**
     * create storable object in repository for parent.
     *
     * @param parentId parent identifier for create object
     * @return new object in UIObject style
     */
    @Override
    public T create(String parentId) {
        return create(_getParent(parentId));
    }

    /**
     * create storable object in repository for parent.
     *
     * @param parent parent
     * @return new object in UIObject style
     */
    @Override
    public T create(Storable parent) {
        U object = manager().create(parent);
        object.setName("New " + object.getClass().getSimpleName());
        LOGGER.info("Storable {} is created", object);
        object.store();
        object.flush();
        return _newInstanceTClass(object);
    }

    @Override
    public T create(String parentId, BigInteger projectId) {
        U object = manager().create(_getParent(parentId));
        object.setName("New " + object.getClass().getSimpleName());
        object.setProjectId(projectId);
        LOGGER.info("Storable {} is created", object);
        object.store();
        object.flush();
        return _newInstanceTClass(object);
    }

    /**
     * create new object.
     *
     * @param parentId    parent identifier
     * @param name        name identifier
     * @param type        type identifier
     * @param description description identifier
     * @param labels      labels identifier
     * @return new generic object
     */
    @Override
    public T create(String parentId, String name, String type, String description, List<String> labels) {
        U object = manager().create(_getParent(parentId), name, type, description, labels);
        object.setName("New " + object.getClass().getSimpleName());
        LOGGER.info("Storable {} is created", object);
        object.store();
        object.flush();
        return _newInstanceTClass(object);
    }

    /**
     * create storable object in repository for parent.
     *
     * @param parentId parent identifier for create object
     * @param type     type of create object
     * @return new object in UIObject style
     */
    @Override
    public T create(String parentId, String type) {
        U object = manager().create(_getParent(parentId), type);
        object.setName("New " + object.getClass().getSimpleName());
        LOGGER.info("Storable {} is created", object);
        object.store();
        object.flush();
        return _newInstanceTClass(object);
    }

    public T create(String parentId, String name, String type) {
        U object = manager().create(_getParent(parentId), name, type);
        LOGGER.info("Storable {} is created", object);
        object.store();
        object.flush();
        return _newInstanceTClass(object);
    }

    /**
     * update older object in repository.
     *
     * @param uiObject have new param for update object
     * @return updated UIObject
     */
    @Override
    public T update(T uiObject) {
        return updateUIObject(manager().getById(uiObject.getId()), uiObject);
    }

    protected T updateUIObject(U object, T uiObject) {
        beforeStoreUpdated(object, uiObject);
        return storeUpdated(object, uiObject);
    }

    protected void beforeStoreUpdated(U object, T uiObject) {
        ControllerHelper.throwExceptionIfNull(object, uiObject.getName(), uiObject.getId(), _getGenericUClass());
        checkVersion(object, uiObject);
        UIHelper.updateObject(uiObject, object);
        _beforeUpdate(uiObject, object);
    }

    protected void checkVersion(U object, T uiObject) {
        if (!uiObject.calcIsVersionActual(object)) {
            throw new NotActualVersionException(
                    uiObject.getName(),
                    String.valueOf(uiObject.getVersion()),
                    String.valueOf(object.getVersion())
            );
        }
    }

    protected T storeUpdated(U object, T uiObject) {
        object.store();
        object.flush();
        uiObject = _newInstanceTClass(object);
        return uiObject;
    }

    @Override
    public UIResult update(List<T> uiObjects) {
        return null;
    }

    /**
     * Deletes all objects in the repository from the resulting list.
     *
     * @param objectsToDelete list by delete
     */
    @Override
    public List<UIObject> delete(UIIds objectsToDelete) {
        List<UIObject> uiObjects = new ArrayList<>();
        for (String string : objectsToDelete.getIds()) {
            U object = manager().getById(string);
            uiObjects.add(new UIObject(object));
            delete(object);
            LOGGER.info("Storable {} is deleted", object);
        }
        return uiObjects;
    }

    /**
     * Deletes all objects in the repository from the resulting list.
     *
     * @param objectsToDelete list by delete
     */
    @Override
    public void delete(Collection<T> objectsToDelete) {
        for (UIObject uiObject : objectsToDelete) {
            delete(manager().getById(uiObject.getId()));
        }
    }

    /**
     * if set true all object will be UIObject.
     *
     * @param simple simplification parameter
     */
    public void setSimple(boolean simple) {
        isSimple = simple;
    }

    public void delete(U object) {
        if (object != null) {
            _deleteSubObjects(object);
            object.remove();
        }
    }

    /**
     * returns all objects of the generic type and parent if it suitable in repository.
     * if parent is null returns all.
     *
     * @param parentId parent identifier
     * @param param    parameters for check suitable
     * @return a list of generic objects
     */
    protected List<? extends UIObject> getAllSuitable(String parentId, String... param) {
        if (Objects.nonNull(parentId)) {
            return asListUIObject(manager().getAllByParentId(parentId), true, isSimple, param);
        }
        Collection<U> allObjects = new ArrayList<>();
        if (isMultiTenant) {
            allObjects.addAll(getAllPerCluster());
            return asListUIObject(allObjects, false, isSimple, param);
        }
        allObjects.addAll(manager().getAll());
        return asListUIObject(allObjects, false, isSimple, param);
    }

    /**
     * returns all objects of the UIObject  in repository.
     *
     * @return a list of UIObjects
     */
    protected List<? extends UIObject> getAllSimple() {
        Collection<U> allObjects = new ArrayList<>();
        if (isMultiTenant) {
            allObjects.addAll(getAllPerCluster());
            return asListUIObject(allObjects, false, isSimple);
        }
        allObjects.addAll(manager().getAll());
        return asListUIObject(allObjects, false, isSimple);
    }

    /**
     * Delete objects in repository and get result about it.
     *
     * @param ignoreUsages    indicates whether to ignore usages
     * @param objectsToDelete list by delete
     * @return result map
     */
    protected List<List<UIObject>> delete(Boolean ignoreUsages, UIIds objectsToDelete) {
        List<UIObject> deletedUiObjects = new ArrayList<>();
        List<UIObject> usedUiObjects = new ArrayList<>();
        Map<String, Map<String, String>> result = Maps.newHashMap();
        for (String string : objectsToDelete.getIds()) {
            U uiObject = manager().getById(string);
            if (haveUsages(uiObject, result, ignoreUsages)) {
                usedUiObjects.add(new UIObject(uiObject));
                continue;
            }
            deletedUiObjects.add(new UIObject(uiObject));
            delete(uiObject);
            LOGGER.info("Storable {} is deleted", uiObject);
        }
        List<List<UIObject>> allObjects = new ArrayList<>();
        allObjects.add(deletedUiObjects);
        allObjects.add(usedUiObjects);
        return allObjects;
    }

    /**
     * do something before update the object.
     *
     * @param uiObject have param for update
     * @param object   updatable object
     * @return updatable object
     */
    protected U _beforeUpdate(T uiObject, U object) {
        return object;
    }

    /**
     * delete all sub objects in deleted the object.
     *
     * @param object parent object
     */
    protected void _deleteSubObjects(U object) {
        //default is nothing
    }

    protected boolean _isObjectSuitable(U object, String... param) {
        return true; //default is true
    }

    /**
     * get manager of repository for generic class.
     *
     * @return object manager
     */
    protected ObjectManager<U> manager() {
        return ControllerHelper.getManager(_getGenericUClass());
    }

    /**
     * must be return generic UClass.
     *
     * @return UClass
     */
    protected abstract Class<U> _getGenericUClass();

    /**
     * create new UIObject class.
     *
     * @param object storable for create an object extends UIObject
     * @return an object extends UIObject
     */
    protected abstract T _newInstanceTClass(U object);

    /**
     * logic how to find a parent for id.
     *
     * @param parentId parent identify
     * @return parent
     */
    protected abstract Storable _getParent(String parentId);

    private UIObject newInstanceTClass(boolean isAdded, boolean isSimple, U object) {
        if (isAdded) {
            if (isSimple) {
                return new UIObject(object, false);
            } else {
                return _newInstanceTClass(object);
            }
        }
        return null;
    }

    public List<? extends UIObject> asListUIObject(Collection<? extends U> collection, boolean checkSuitable,
                                                   boolean isSimple, String... param) {
        ArrayList<UIObject> arrayList = Lists.newArrayListWithCapacity(collection.size());
        for (U object : collection) {
            boolean isAdded = false;
            if (checkSuitable) {
                if (_isObjectSuitable(object, param)) {
                    isAdded = true;
                }
            } else {
                isAdded = true;
            }
            arrayList.add(newInstanceTClass(isAdded, isSimple, object));
        }
        return arrayList;
    }

    public String usageInfoListAsString(Collection<UsageInfo> usageInfoList) {
        return usageInfoList.stream()
                .map(usage -> usage.getReferer().toString())
                .collect(Collectors.joining("\n"));
    }
}
