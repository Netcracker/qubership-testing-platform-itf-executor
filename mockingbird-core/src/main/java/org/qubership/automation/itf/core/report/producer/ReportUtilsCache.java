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

package org.qubership.automation.itf.core.report.producer;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class ReportUtilsCache {

    private static volatile ReportUtilsCache executorServiceCache;

    private Map<BigInteger, ExecutorService> executorServiceByProject = Maps.newConcurrentMap();
    private Map<BigInteger, ObjectMapper> mapperByProject = Maps.newConcurrentMap();

    private ReportUtilsCache() {
        if (executorServiceCache != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static ReportUtilsCache getInstance() {
        if (executorServiceCache == null) {
            synchronized (ReportUtilsCache.class) {
                if (executorServiceCache == null) executorServiceCache = new ReportUtilsCache();
            }
        }
        return executorServiceCache;
    }

    public ExecutorService getExecutorService(BigInteger projectId) {
        return executorServiceByProject.get(projectId);
    }

    public void addExecutorService(BigInteger projectId, ExecutorService executorService) {
        executorServiceByProject.put(projectId, executorService);
    }

    public ObjectMapper getMapper(BigInteger projectId) {
        return mapperByProject.get(projectId);
    }

    public void addMapper(BigInteger projectId, ObjectMapper mapper) {
        mapperByProject.put(projectId, mapper);
    }
}
