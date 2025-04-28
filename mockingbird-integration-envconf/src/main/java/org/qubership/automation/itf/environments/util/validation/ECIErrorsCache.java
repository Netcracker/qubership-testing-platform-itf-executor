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

package org.qubership.automation.itf.environments.util.validation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

public class ECIErrorsCache {
    private static final ECIErrorsCache INSTANCE = new ECIErrorsCache();
    private static final int EXPIRED_TIMEOUT = 2;
    private static final Cache<UUID, List<ECIValidationError>> errorsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRED_TIMEOUT, TimeUnit.MINUTES)
            .build();

    public ECIErrorsCache() {
    }


    public static ECIErrorsCache getInstance() {
        return INSTANCE;
    }

    public Cache<UUID, List<ECIValidationError>> getErrorsCache() {
        return errorsCache;
    }

    public void put(UUID eciSessionId, String loadedEntityName, String loadedEntityType, String error,
                    ValidationLevel validationLevel) {
        List<ECIValidationError> errors = ECIErrorsCache.getInstance().getErrorsCache().asMap().get(eciSessionId);
        ECIValidationError eciValidationError = new ECIValidationError(eciSessionId, loadedEntityName,
                loadedEntityType, error, validationLevel);
        if (errors != null) {
            errors.add(eciValidationError);
        } else {
            errors = Lists.newArrayList(eciValidationError);
            errorsCache.put(eciSessionId, errors);
        }
    }
}
