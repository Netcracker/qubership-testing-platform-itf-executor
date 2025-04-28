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

package org.qubership.automation.itf.transport.ldap.outbound;

public interface LdapConstants {

    String INITIAL_CONTEXT_FACTORY = "initialContextFactory";
    String PROVIDER_URL = "providerUrl";
    String AUTHENTICATION = "authentication";
    String PRINCIPAL = "principal";
    String CREDENTIALS = "credentials";
    String ADDITIONAL_JNDI_PROPERTIES = "addJndiProps";
    String OUTPUT_FORMAT = "outputFormat";
    String RESPONSE_CODE = "responseCode";

    String LDAP_DATASOURCE = "itfldap";
    String DEFAULT_OUTPUT_FORMAT = "LDIF";
}
