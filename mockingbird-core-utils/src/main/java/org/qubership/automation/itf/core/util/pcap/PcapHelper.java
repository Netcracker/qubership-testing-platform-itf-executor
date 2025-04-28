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

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.TCP_DUMP_FOLDER;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT_DEFAULT_VALUE;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.services.CoreServices;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PcapHelper {

    public static String TCPDUMP_FILTER_KEY = "tcpdumpFilter";
    public static String TCPDUMP_NETWORK_INTERFACE_NAME_KEY = "networkInterfaceName";
    public static String TCPDUMP_PACKET_COUNT_KEY = "packetCount";

    public static void createPcapManager(String id, BigInteger projectId, String niName, String filter,
                                         String packetCount) {
        if (StringUtils.isBlank(niName) || StringUtils.isBlank(packetCount)) {
            log.error("Required parameters are missed - TCP dump is disabled. Network interface name: {}, Packet "
                    + "count: {}", niName, packetCount);
        }
        String dumpfilePath = String.format("%s/%s/%s.pcap", Config.getConfig().getString(TCP_DUMP_FOLDER),
                projectId.toString(), id);
        int maxPacketCount;
        try {
            maxPacketCount = Integer.parseInt(packetCount);
        } catch (NumberFormatException e) {
            log.error("Error while parsing the packet count parameter - default value will be used. Error: " + e);
            maxPacketCount = CoreServices.getProjectSettingsService().getInt(projectId, TCPDUMP_PACKET_COUNT_DEFAULT,
                    Integer.parseInt(TCPDUMP_PACKET_COUNT_DEFAULT_DEFAULT_VALUE));
        }
        PcapManagersCache.getInstance().add(id,
                new PcapManager(dumpfilePath, niName, filter, maxPacketCount));
    }

    public static String startTcpDumpCreating(String id) {
        String dumpfilePath = "";
        PcapManager pcapManager = PcapManagersCache.getInstance().getPcapManager(id);
        if (pcapManager != null) {
            log.info("Start of creating the tcp-dump for context: " + id);
            PcapThread pcapThread = new PcapThread(pcapManager, id);
            PcapThreadsCache.getInstance().add(id, pcapThread);
            pcapThread.start();
            dumpfilePath = Config.getConfig().getRunningUrl() + "tcpdump/" + id + ".pcap";
        }
        return dumpfilePath;
    }

    public static void stopTcpDumpCreating(String id) {
        PcapThread pcapThread = PcapThreadsCache.getInstance().get(id);
        if (pcapThread != null) {
            log.info("End of creating tcp-dump for context: " + id);
            pcapThread.stopThread();
        }
    }

    /*
     *   Unused. It's incorrect in a cloud, so replaced with constructing URL via Config.getConfig().getRunningUrl().
     *   Will be deleted soon.
     * */
    private static String createFilepath(String filename) {
        String host;
        try {
            host = Inet4Address.getLocalHost().getCanonicalHostName();
            String port = System.getProperties().getProperty("port");
            if (StringUtils.isEmpty(port)) {
                port = "8080";
            }
            //noinspection HttpUrlsUsage
            return String.format("http://%s:%s/%s", host, port, filename);
        } catch (UnknownHostException e) {
            log.error("Can't identify the host. Link to the created file with TCPDump will not be created. "
                    + "You can find the file in ITF-folder.");
            return StringUtils.EMPTY;
        }
    }
}
