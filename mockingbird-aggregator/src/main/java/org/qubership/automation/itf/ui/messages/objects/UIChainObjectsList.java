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

package org.qubership.automation.itf.ui.messages.objects;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.qubership.automation.itf.core.hibernate.spring.managers.executor.CallChainObjectManager;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;

public class UIChainObjectsList {
    private List<UIChainObject> allChains;

    public UIChainObjectsList(BigInteger projectId) {
        List<UIChainObject> list = new ArrayList<>();
        List<Object[]> manualList = CoreObjectManager.getInstance()
                .getSpecialManager(CallChain.class, CallChainObjectManager.class)
                .getAllIdsAndNamesByProjectId(projectId);
        for (Object[] array : manualList) {
            list.add(new UIChainObject(array[0].toString(), array[1].toString()));
        }
        this.allChains = list;
    }

    public List<UIChainObject> getAllChains() {
        return allChains;
    }

    public void setChains(List<UIChainObject> allChains) {
        this.allChains = allChains;
    }
}
