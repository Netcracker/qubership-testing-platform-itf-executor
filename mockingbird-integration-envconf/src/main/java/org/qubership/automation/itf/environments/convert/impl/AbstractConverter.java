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

package org.qubership.automation.itf.environments.convert.impl;

import java.math.BigInteger;
import java.util.UUID;

import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvConfigurationManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.convert.Converter;
import org.qubership.automation.itf.environments.object.ECEntity;
import org.qubership.automation.itf.environments.object.impl.ECServer;

abstract class AbstractConverter<T extends EciConfigurable, V extends ECEntity<? extends EciConfigurable>>
        implements Converter<T, V> {

    public void setMainParams(T storable, V ecEntity) {
        storable.setName(ecEntity.getName());
        storable.setDescription(ecEntity.getDescription());
    }

    public void setECIParams(T storable, V ecEntity) {
        storable.setEcId(ecEntity.getEcId());
        if (ecEntity.getEcProjectId() != null) {
            storable.setEcProjectId(ecEntity.getEcProjectId());
        }
    }

    public T convert(V ecEntity, BigInteger parentId, UUID eciSessionId) {
        Storable parent = CoreObjectManager.getInstance().getManager(ecEntity.getParentClass()).getById(parentId);
        return parent != null ? convert(ecEntity, parent, eciSessionId) : null;
    }

    public abstract T convert(V ecEntity, Storable parent, UUID eciSessionId, Object... objects);

    public void convert(V ecEntity, UUID eciSessionId) {
    }

    public T getConfigurableEntity(V objectToConvert) {
        return (objectToConvert instanceof ECServer)
                ? (T) CoreObjectManager.getInstance().getSpecialManager(objectToConvert.getGenericType(),
                EnvConfigurationManager.class).getByEcId(objectToConvert.getEcId(), objectToConvert.getName(),
                ((ECServer) objectToConvert).getUrl())
                : (T) CoreObjectManager.getInstance().getSpecialManager(objectToConvert.getGenericType(),
                EnvConfigurationManager.class).getByEcId(objectToConvert.getEcId());
    }
}
