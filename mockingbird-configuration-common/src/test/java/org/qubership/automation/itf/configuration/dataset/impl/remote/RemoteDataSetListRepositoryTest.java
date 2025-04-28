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

package org.qubership.automation.itf.configuration.dataset.impl.remote;

import org.junit.Ignore;
import org.junit.Test;
import org.qubership.automation.itf.core.model.dataset.DataSetList;

import com.google.common.base.Stopwatch;

public class RemoteDataSetListRepositoryTest {

    @Ignore
    @Test
    public void testGetAllDatasets() {
//        for (int i = 0; i < 50; i++) {
        Stopwatch started = Stopwatch.createStarted();
        RemoteDataSetListRepository repository = new RemoteDataSetListRepository(null);
        DataSetList list;
        list = repository.getByNatureId("50eb69ee-5051-433c-83eb-60b5dfe2eb20_5c868909-55de-4e8d-bb80"
                + "-bd300eff06b9", null);
        System.out.println(list);
        System.out.println(started.stop());
//        }
    }
}
