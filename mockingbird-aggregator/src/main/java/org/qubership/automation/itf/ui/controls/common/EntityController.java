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
import java.util.Collection;
import java.util.List;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;

/**
 * Interface for entities storable objects.
 *
 * @param <T> Object of UI style
 */
public interface EntityController<T extends UIObject> {
    /**
     * returns all objects.
     *
     * @return a list of generic objects
     */
    List<? extends UIObject> getAll();

    /**
     * returns all objects for parent.
     *
     * @param parentId parent identifier
     * @return a list of generic objects
     */
    List<? extends UIObject> getAll(String parentId);

    /**
     * returns one object.
     *
     * @param id identifier
     * @return generic object by id
     */
    T getById(String id);

    /**
     * create new object.
     *
     * @param parentId parent identifier
     * @return new generic object
     */
    T create(String parentId);

    T create(Storable parent);

    T create(String parentId, BigInteger projectId);

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
    T create(String parentId, String name, String type, String description, List<String> labels);

    /**
     * create new object.
     *
     * @param parentId parent identifier
     * @param type     type of create object
     * @return new generic object
     */
    T create(String parentId, String type);

    /**
     * create new object.
     *
     * @param parentId parent identifier
     * @param name     name of create object
     * @param type     type of create object
     * @return new generic object
     */
    T create(String parentId, String name, String type);

    /**
     * update object.
     *
     * @param uiObject generic object with new param
     * @return updated generic object
     */
    T update(T uiObject);

    /**
     * update objects.
     *
     * @param uiObjects generic objects with new param
     * @return updated generic object
     */
    UIResult update(List<T> uiObjects);

    /**
     * delete objects.
     *
     * @param objectsToDelete ID array for delete
     */
    List<UIObject> delete(UIIds objectsToDelete);

    /**
     * delete objects.
     *
     * @param objectsToDelete UIObject collection  for delete
     */
    void delete(Collection<T> objectsToDelete);
}
