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

package org.qubership.automation.itf.integration.atp.util;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.CallChainObjectManager;
import org.qubership.automation.itf.core.model.IdNamePair;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.model.AdvancedValue;
import org.qubership.automation.itf.integration.atp.model.ArgumentValue;

import com.google.common.collect.Lists;

public class DefinitionBuilder {

    public static ArgumentValue getAvailableCallchainsList(BigInteger projectId) {
        return newArgumentValue(ATPActionConstants.CALLCHAIN_INDEX.intValue(),
                createAdvancedValueListWithCallChains(projectId));
    }

    public static ArgumentValue getAvailableDatasetsEmptyList() {
        return newArgumentValue(ATPActionConstants.DATASET_INDEX.intValue(), Lists.newArrayList());
    }

    public static ArgumentValue getAvailableCallchainLabelList() {
        return newArgumentValue(ATPActionConstants.LABEL_INDEX.intValue(), Lists.newArrayList());
    }

    public static ArgumentValue getAvailableValuesList(int argumentNumber, String[] values) {
        return newArgumentValue(argumentNumber, createAdvancedValueListWithStringValues(values));
    }

    static String getBackUrl() {
        return Config.getConfig().getRunningUrl() + ATPActionConstants.ITF_TC_URL_TEMPLATE.stringValue();
    }

    public static List<AdvancedValue> createAdvancedValueListWithCallChains(BigInteger projectId) {
        List<AdvancedValue> result = Lists.newArrayList();
        if (projectId != null) {
            List<IdNamePair> list = CoreObjectManager.getInstance()
                    .getSpecialManager(CallChain.class, CallChainObjectManager.class).getSimpleListByProject(projectId);
            for (IdNamePair elem : list) {
                AdvancedValue data = new AdvancedValue();
                data.setId(elem.getId());
                data.setValue(elem.getName());
                result.add(data);
            }
        }
        return result;
    }

    private static List<AdvancedValue> createAdvancedValueListWithStringValues(String[] values) {
        List<AdvancedValue> result = Lists.newArrayList();
        for (String value : values) {
            AdvancedValue data = new AdvancedValue();
            data.setId(StringUtils.EMPTY);
            data.setValue(value);
            result.add(data);
        }
        return result;
    }

    private static ArgumentValue newArgumentValue(int argumentNumber, List<AdvancedValue> availableValues) {
        ArgumentValue argumentValue = new ArgumentValue();
        argumentValue.setArgumentNumber(argumentNumber);
        argumentValue.setCanBeCustom(false);
        argumentValue.setBackUrl(getBackUrl());
        argumentValue.setAdvValues(availableValues);
        return argumentValue;
    }
}
