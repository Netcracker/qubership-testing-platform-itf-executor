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

package org.qubership.automation.itf.transport.http2;

public interface HTTP2Constants {
    String ENDPOINT = "endpoint";
    String METHOD = "method";
    String HEADERS = "headers";
    String REMOTE_HOST = "remoteIp";
    String REMOTE_PORT = "remotePort";
    String RESPONSE_CODE = "responseCode";
    String ADD_PROJECTUUID_ENDPOINT_PREFIX = "addProjectUuidEndpointPrefix";
    String REMOTE_HOST_DESCRIPTION = "Remote host IP or name";
    String REMOTE_PORT_DESCRIPTION = "Remote port";
    String LOCAL_HOST_DESCRIPTION = "Local host IP or name";
    String LOCAL_PORT_DESCRIPTION = "Local port";
    String HEADERS_DESCRIPTION = "SomeHeader=SomeValue\nContent-Type=text/html";
    String ENDPOINT_URI_DESCRIPTION = "Endpoint URI, e.g. /mb/test/endpoint";
    String METHOD_DESCRIPTION = "HTTP Method";
    String ADD_PROJECTUUID_ENDPOINT_PREFIX_DESCRIPTION = "Add Project UUID Prefix into Endpoint? - Yes (default) /No";
}
