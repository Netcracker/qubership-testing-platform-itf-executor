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

package org.qubership.automation.itf.transport.diameter.interceptors;

import static org.qubership.automation.diameter.interceptor.InterceptorTypes.SNR;

import java.util.Objects;

public class SNRInterceptor extends ExternalInterceptor {

    public SNRInterceptor() {
        super();
        setType(SNR);
    }

    public SNRInterceptor(Object transportId, Object tcContextId) {
        super(transportId, tcContextId, SNR);
    }

    @Override
    boolean isApplicable(String message) {
        return Helper.checkRequestTagAndSession(message, "<SNR>", getSessionId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SNRInterceptor that = (SNRInterceptor) o;
        return Objects.equals(getSessionId(), that.getSessionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getSessionId());
    }
}
