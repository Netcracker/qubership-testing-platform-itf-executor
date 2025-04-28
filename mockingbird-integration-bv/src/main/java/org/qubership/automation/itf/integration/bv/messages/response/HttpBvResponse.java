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

import static org.qubership.automation.itf.integration.bv.utils.BvResponseProcessor.parseResponse;

import org.apache.commons.lang3.StringUtils;

public class HttpBvResponse {
    private BvResponseData bvResponseData = null;
    private int httpResponseCode = -1;
    private int bvStatusCode = -1;
    private boolean success;
    private String httpResponseBody;
    private String bvStatusMessage;

    public HttpBvResponse(int httpResponseCode, boolean httpSuccess, String httpResponseBody) {
        this.httpResponseCode = httpResponseCode;
        this.httpResponseBody = httpResponseBody;
        this.success = httpSuccess;
        if (httpSuccess) {
            bvResponseData = parseResponse(httpResponseBody);
            if (bvResponseData != null) {
                String statusCode = bvResponseData.getStatusCode();
                if (!StringUtils.isBlank(statusCode)) {
                    bvStatusCode = Integer.parseInt(statusCode);
                    if (bvStatusCode < 10000 || bvStatusCode >= 20000) {
                        this.success = false;
                    }
                }
                bvStatusMessage = bvResponseData.getStatusMessage();
            }
        }
    }

    public HttpBvResponse() {
    }

    public BvResponseData getBvResponseData() {
        return bvResponseData;
    }

    public void setBvResponseData(BvResponseData bvResponseData) {
        this.bvResponseData = bvResponseData;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getHttpResponseBody() {
        return httpResponseBody;
    }

    public void setHttpResponseBody(String httpResponseBody) {
        this.httpResponseBody = httpResponseBody;
    }

    public int getBvStatusCode() {
        return bvStatusCode;
    }

    public void setBvStatusCode(int bvStatusCode) {
        this.bvStatusCode = bvStatusCode;
    }

    public String getBvStatusMessage() {
        return bvStatusMessage;
    }

    public void setBvStatusMessage(String bvStatusMessage) {
        this.bvStatusMessage = bvStatusMessage;
    }
}
