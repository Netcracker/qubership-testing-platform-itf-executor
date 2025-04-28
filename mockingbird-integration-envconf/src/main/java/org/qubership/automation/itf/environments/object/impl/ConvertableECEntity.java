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

package org.qubership.automation.itf.environments.object.impl;

import java.math.BigInteger;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.environments.object.ECEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConvertableECEntity<T extends EciConfigurable> extends IdentifiedECEntity implements ECEntity<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ConvertableECEntity.class);
    private String name;
    private String description;
    private BigInteger sourceEntityId;
    private BigInteger sourceEntityParentId;

    private Class<T> genericType;
    private Class<? extends Storable> parentClass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class<T> getGenericType() {
        return genericType;
    }

    public void setGenericType(Class<T> genericType) {
        this.genericType = genericType;
    }

    public Class<? extends Storable> getParentClass() {
        return parentClass;
    }

    public void setParentClass(Class<? extends Storable> parentClass) {
        this.parentClass = parentClass;
    }

    public BigInteger getSourceEntityId() {
        return sourceEntityId;
    }

    public void setSourceEntityId(BigInteger sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
    }

    public BigInteger getSourceEntityParentId() {
        return sourceEntityParentId;
    }

    public void setSourceEntityParentId(BigInteger sourceEntityParentId) {
        this.sourceEntityParentId = sourceEntityParentId;
    }
}
