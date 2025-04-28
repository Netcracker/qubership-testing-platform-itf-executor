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

package org.qubership.automation.itf.core.util.pcap;

import java.io.IOException;
import java.util.List;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.util.NifSelector;

public class CustomNifSelector extends NifSelector {

    private String networkInterfaceName;

    public CustomNifSelector(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
    }

    protected PcapNetworkInterface doSelect(List<PcapNetworkInterface> nifs) throws IOException {
        for (PcapNetworkInterface nif : nifs) {
            if (nif.getName().equals(networkInterfaceName)) {
                return nif;
            }
        }
        return null;
    }
}
