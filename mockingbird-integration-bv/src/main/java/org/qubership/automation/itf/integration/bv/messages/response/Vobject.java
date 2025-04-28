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

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

public class Vobject {
    List<Vobject> childs;
    private String name;
    private Data value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Vobject> getChilds() {
        return childs;
    }

    public void setChilds(List<Vobject> childs) {
        this.childs = childs;
    }

    public Data getValue() {
        return value;
    }

    public void setValue(Data value) {
        this.value = value;
    }

    public String getDecodedValue() {
        if (this.value == null) {
            return "";
        }
        try {
            return this.value.decodeContent();
        } catch (UnsupportedEncodingException e) {
            return ""; // As 0 iteration
        }
    }

    private class Data {
        String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String decodeContent() throws UnsupportedEncodingException {
            if (StringUtils.isBlank(this.content)) {
                return "";
            }
            return new String(Base64.decodeBase64(this.content), "UTF-8");
        }
    }
}
