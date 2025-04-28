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

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.util.Collection;
import java.util.Map;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.usage.UsageInfo;

import com.google.common.collect.Maps;

public abstract class AbstractStorableController<T extends Storable> {

    /*TODO decompose the method*/
    protected boolean haveUsages(T object, Map<String, Map<String, String>> result, Boolean ignoreUsages) {
        Map<String, String> successMap = result.get("success");
        Map<String, String> failureMap = result.get("failure");
        if (successMap == null) {
            successMap = Maps.newHashMap();
            result.put("success", successMap);
        }
        if (failureMap == null) {
            failureMap = Maps.newHashMap();
            result.put("failure", failureMap);
        }
        if (!ignoreUsages) {
            Collection<UsageInfo> uInfo = getManager(object.getClass()).findUsages(object);
            if (uInfo != null && !uInfo.isEmpty()) {
                StringBuilder str = new StringBuilder();
                for (UsageInfo item : uInfo) {
                    str.append("as [").append(item.getProperty()).append("] in the object: ")
                            .append(item.getReferer().toString()).append("\n");
                }
                failureMap.put(object.getID().toString(),
                        "Object " + object.getName() + " [" + object.getID() + "] " + "can't be deleted because it "
                                + "is" + " used:\n" + str.toString() + ". TRYING TO DELETE MULTIPLE " + "CROSS"
                                + "-REFERENCED " + "OBJECTS AT ONCE MAY PRODUCE TABLE LOCKS.");
                if (successMap.isEmpty()) {
                    result.remove("success");
                }
                return true;
            }
        }
        successMap.put(object.getID().toString(),
                "Object " + object.getName() + " [" + object.getID() + "] can be " + "deleted " + ((ignoreUsages)
                        ? "(usages are not checked)" : "(no usages)"));
        if (failureMap.isEmpty()) {
            result.remove("failure");
        }
        return false;
    }
}
