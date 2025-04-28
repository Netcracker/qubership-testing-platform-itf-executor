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

package org.qubership.automation.itf.ui.messages.objects.transport;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.FILE_DIRECTORY_PATTERN;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.FILE_DIRECTORY_RELATIVE_PATH_GROUP_NUMBER;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.parents.UINamedObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UIProperty extends UINamedObject implements Comparable<UIProperty> {
    private String userName;
    private String value;
    private UIObject referenceValue;
    private boolean boolValue;
    private String optional;
    private String description;
    private String typeName;
    private String inputType;
    private String overridden;
    private String inheritedValue;
    private String select;
    private String referenceClass;
    private String[] options;
    private int order;
    private String filePathDirectoryType;
    private String uiCategory;
    private String validatePattern;

    public UIProperty() {
    }

    public UIProperty(PropertyDescriptor descriptor) {
        setName(descriptor.getShortName());
        setUserName(descriptor.getLongName());
        setDescription(descriptor.getDescription());
        setOptional(String.valueOf(descriptor.isOptional()));
        setSelect(Boolean.toString(descriptor.isSelect()));
        setOptions(ArrayUtils.clone(descriptor.getOptions()));
        setTypeName(descriptor.getTypeName());
        setOrder(descriptor.getOrder());
        setFilePathDirectoryType(descriptor.getFileDirectoryType());
        setUiCategory(descriptor.getUiCategory());
        setValidatePattern(descriptor.getValidatePattern());
        try {
            Class<?> aClass = Class.forName(descriptor.getTypeName());
            if (Map.class.isAssignableFrom(aClass)) {
                inputType = "map";
            } else if (Collection.class.isAssignableFrom(aClass)) {
                inputType = "list";
            } else if (Storable.class.isAssignableFrom(aClass)) {
                inputType = "reference";
                referenceClass = descriptor.getTypeName();
            } else if (descriptor.loadTemplate()) {
                inputType = "reference";
                referenceClass = Template.class.getName();
            } else if (File.class.isAssignableFrom(aClass)) {
                inputType = "file";
            } else if (Boolean.class.isAssignableFrom(aClass)) {
                inputType = "boolean";
            } else if (Integer.class.isAssignableFrom(aClass)) {
                inputType = "number";
            } else {
                inputType = "string";
            }
        } catch (ClassNotFoundException e) {
            inputType = "string";
        }
    }

    public UIProperty(InterceptorPropertyDescriptor descriptor) {
        setName(descriptor.getName());
        setUserName(descriptor.getLongname());
        setDescription(descriptor.getDescription());
        setInputType(descriptor.getInputType());
        setOptions(descriptor.getOptions());
        setValue(descriptor.getValue());
        setOptional(String.valueOf(descriptor.isOptional()));
    }

    public UIProperty(PropertyDescriptor descriptor, String value) {
        this(descriptor);
        if ("boolean".equals(inputType)) {
            setBoolValue(Boolean.parseBoolean(value));
        } else {
            String propertyValue;
            if (value != null && StringUtils.isNotEmpty(descriptor.getFileDirectoryType())) {
                Matcher matcher = FILE_DIRECTORY_PATTERN.matcher(value);
                if (matcher.find()) {
                    propertyValue = matcher.group(FILE_DIRECTORY_RELATIVE_PATH_GROUP_NUMBER);
                } else {
                    log.info(String.format("Error while getting the value for file attribute %s from %s. Original "
                            + "value will be set.", descriptor.getLongName(), value));
                    propertyValue = value;
                }
            } else {
                propertyValue = value;
            }
            setValue(propertyValue);
            setInheritedValue(propertyValue);
        }
    }

    public UIProperty(PropertyDescriptor descriptor, UIObject value) {
        this(descriptor);
        setReferenceValue(value);
        setReferenceClass(value.getClassName());
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getValue() {
        if ("boolean".equals(inputType)) {
            return Boolean.toString(boolValue);
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOptional() {
        return optional;
    }

    public void setOptional(String optional) {
        this.optional = optional;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOverridden() {
        return overridden;
    }

    public void setOverridden(String overridden) {
        this.overridden = overridden;
    }

    public String getInheritedValue() {
        return inheritedValue;
    }

    public void setInheritedValue(String inheritedValue) {
        this.inheritedValue = inheritedValue;
    }

    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public UIObject getReferenceValue() {
        return referenceValue;
    }

    public void setReferenceValue(UIObject referenceValue) {
        this.referenceValue = referenceValue;
    }

    public String getReferenceClass() {
        return referenceClass;
    }

    public void setReferenceClass(String referenceClass) {
        this.referenceClass = referenceClass;
    }

    public boolean isBoolValue() {
        return boolValue;
    }

    public void setBoolValue(boolean boolValue) {
        this.boolValue = boolValue;
    }

    @Override
    public int compareTo(UIProperty property) {
        return Integer.compare(this.order, property.order);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getFilePathDirectoryType() {
        return filePathDirectoryType;
    }

    public void setFilePathDirectoryType(String filePathDirectoryType) {
        this.filePathDirectoryType = filePathDirectoryType;
    }

    public String getUiCategory() {
        return uiCategory;
    }

    public void setUiCategory(String uiCategory) {
        this.uiCategory = uiCategory;
    }

    public String getValidatePattern() {
        return validatePattern;
    }

    public void setValidatePattern(String validatePattern) {
        this.validatePattern = validatePattern;
    }
}
