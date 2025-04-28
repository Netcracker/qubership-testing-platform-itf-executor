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

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class BulkUpdateRequest {

    private String position;
    private String textData;
    private String selectedOperation;
    private boolean childrenFolders;
    private ArrayList<String> checkedObjectsIdList;
    private ArrayList<String> checkedObjectsClassNameList;

    private int intPosition = -1;

    public BulkUpdateRequest() {
    }

    public BulkUpdateRequest(String position, String textData, String selectedOperation, boolean childrenFolders,
                             ArrayList<String> checkedObjectsIdList, ArrayList<String> checkedObjectsClassNameList) {
        this.position = position;
        this.textData = textData;
        this.selectedOperation = selectedOperation;
        this.childrenFolders = childrenFolders;
        this.checkedObjectsIdList = checkedObjectsIdList;
        this.checkedObjectsClassNameList = checkedObjectsClassNameList;
        computeIntPosition();
    }

    public String getSelectedOperation() {
        return selectedOperation;
    }

    public void setSelectedOperation(String selectedOperation) {
        this.selectedOperation = selectedOperation;
    }

    public ArrayList<String> getCheckedObjectsIdList() {
        return checkedObjectsIdList;
    }

    public void setCheckedObjectsIdList(ArrayList<String> checkedObjectsIdList) {
        this.checkedObjectsIdList = checkedObjectsIdList;
    }

    public String getTextData() {
        return textData;
    }

    public void setTextData(String textData) {
        this.textData = textData;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
        computeIntPosition();
    }

    public ArrayList<String> getCheckedObjectsClassNameList() {
        return checkedObjectsClassNameList;
    }

    public void setCheckedObjectsClassNameList(ArrayList<String> checkedObjectsClassNameList) {
        this.checkedObjectsClassNameList = checkedObjectsClassNameList;
    }

    public boolean isChildrenFolders() {
        return childrenFolders;
    }

    public void setChildrenFolders(boolean childrenFolders) {
        this.childrenFolders = childrenFolders;
    }

    public int getIntPosition() {
        return intPosition;
    }

    private void computeIntPosition() {
        if (StringUtils.isNumeric(this.position.substring(1))) {
            try {
                this.intPosition = Integer.parseInt(this.position.substring(1));
            } catch (NumberFormatException ex) {
                // silently ignore
            }
        }
    }
}
