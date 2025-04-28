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

package org.qubership.automation.itf.ui.messages.objects.transport.interceptor;

import java.util.List;

import com.google.common.collect.Lists;

public class UIInterceptorChain {

    private List<UIInterceptor> interceptorChain = Lists.newLinkedList();
    private String parentVersion = "";

    public UIInterceptorChain(List<UIInterceptor> interceptorChain) {
        this.interceptorChain = interceptorChain;
    }

    public UIInterceptorChain() {
    }

    public List<UIInterceptor> getInterceptorChain() {
        return interceptorChain;
    }

    public void setInterceptorChain(List<UIInterceptor> interceptorChain) {
        this.interceptorChain = interceptorChain;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
    }
}
