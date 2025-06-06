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

package org.qubership.automation.itf.integration.bv.messages.response;

import java.util.ArrayList;

public class Context {
    private ArrayList inputParameters;
    private ArrayList values;
    private ArrayList dataSource;
    private ArrayList readedObjects;

    public ArrayList getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(ArrayList inputParameters) {
        this.inputParameters = inputParameters;
    }

    public ArrayList getValues() {
        return values;
    }

    public void setValues(ArrayList values) {
        this.values = values;
    }

    public ArrayList getDataSource() {
        return dataSource;
    }

    public void setDataSource(ArrayList dataSource) {
        this.dataSource = dataSource;
    }

    public ArrayList getReadedObjects() {
        return readedObjects;
    }

    public void setReadedObjects(ArrayList readedObjects) {
        this.readedObjects = readedObjects;
    }
}
