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

package org.qubership.automation.itf.ui.util;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.executor.objects.ei.SimpleItfEntity;

public class AtpExportImportHelper {

    /**
     * Recursive getting folders for ATP Export\Import functionality and saving them to provided
     * List of SimpleItfEntities.
     *
     * @param folder Some type of ITF folder.
     * @param to     List where folders will be saved
     */
    public static void fillSubFolders(Folder<? extends Storable> folder, List<SimpleItfEntity> to) {
        for (Folder<? extends Storable> subFolder : folder.getSubFolders()) {
            SimpleItfEntity data = createSimpleItfEntity(subFolder);
            to.add(data);
            fillSubFolders(subFolder, to);
        }
    }

    /**
     * Create simple ITF object for ATP Export\Import.
     *
     * @param storable ITF storable object (CallChain, System, Some type of Folder)
     * @return {@link SimpleItfEntity} object.
     */
    @Nonnull
    public static SimpleItfEntity createSimpleItfEntity(Storable storable) {
        SimpleItfEntity data = new SimpleItfEntity();
        data.setId(String.valueOf(storable.getID()));
        data.setName(storable.getName());
        data.setParentId(Objects.nonNull(storable.getParent()) ? String.valueOf(storable.getParent().getID()) : "");
        return data;
    }

}
