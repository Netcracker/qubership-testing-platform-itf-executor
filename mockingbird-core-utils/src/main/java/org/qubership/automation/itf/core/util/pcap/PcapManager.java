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

import java.io.File;
import java.io.IOException;

import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcapManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PcapManager.class);

    private PcapHandle handle = null;
    private PcapDumper dumper = null;

    private int snapshotLength = 65536;
    private int readTimeout = 5;

    private int maxPackets;
    private String filter;
    private String filename;

    public PcapManager(String filename, String networkInterfaceName, String filter, int maxPackets) {
        this.filter = filter;
        this.filename = filename;
        this.maxPackets = maxPackets;
        createHandle(networkInterfaceName);
        createDumper();
    }

    public void startPcapFileCreating() {
        if (handle != null && dumper != null) {
            try {
                handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
                PacketListener listener = new PacketListener() {
                    @Override
                    public void gotPacket(Packet packet) {
                        try {
                            dumper.dump(packet, handle.getTimestamp());
                            LOGGER.debug("Packet is added to dump file {}", getFilename());
                        } catch (NotOpenException e) {
                            LOGGER.error("Adding of the packet to dump has been failed. Execution will be interrupted"
                                    + ".", e);
                            interruptExecution();
                        }
                    }
                };
                try {
                    handle.loop(maxPackets, listener);
                } catch (InterruptedException e) {
                    LOGGER.info("Packet listening has been interrupted. Execution will be interupted.");
                    interruptExecution();
                }
                interruptExecution();
            } catch (PcapNativeException | NotOpenException e) {
                LOGGER.error("Error while setting the filter: {}. Execution will be interupted.", filter);
                interruptExecution();
            }
        }
    }

    public String getFilename() {
        return filename;
    }

    public void stopPcapFileCreating() {
        try {
            handle.breakLoop();
        } catch (NotOpenException e) {
            LOGGER.error("Exception while stopping the packet listening");
        }
    }

    private void createHandle(String networkInterfaceName) {
        PcapNetworkInterface networkDevice = getNetworkDevice(networkInterfaceName);
        if (networkDevice != null) {
            try {
                this.handle = networkDevice.openLive(snapshotLength,
                        PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS, readTimeout);
            } catch (PcapNativeException e) {
                LOGGER.error("Error while creating the PcapHandle object: " + e);
            }
        }
    }

    private void createDumper() {
        if (this.handle != null) {
            try {
                new File(filename).createNewFile();
                this.dumper = handle.dumpOpen(filename);
            } catch (PcapNativeException | NotOpenException e) {
                LOGGER.error("Error while creating the PcapDumper object: " + e);
                handle.close();
            } catch (IOException e) {
                LOGGER.error("Error while creating the file for the tcp-dump: " + e);
                handle.close();
            }
        }
    }

    private PcapNetworkInterface getNetworkDevice(String networkInterfaceName) {
        PcapNetworkInterface device = null;
        try {
            device = new CustomNifSelector(networkInterfaceName).selectNetworkInterface();
        } catch (IOException e) {
            LOGGER.error("Error while selecting the device by its name: " + networkInterfaceName);
        }
        return device;
    }

    private void interruptExecution() {
        dumper.close();
        handle.close();
        Thread.currentThread().interrupt();
    }
}
