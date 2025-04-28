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

public class PcapThread extends Thread {

    private PcapManager pcapManager;
    private String tcId;

    public PcapThread(PcapManager pcapManager, String tcId) {
        this.pcapManager = pcapManager;
        this.tcId = tcId;
    }

    @Override
    public void run() {
        pcapManager.startPcapFileCreating();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            PcapThreadsCache.getInstance().delete(tcId);
            PcapManagersCache.getInstance().delete(tcId);
        }
    }

    public void stopThread() {
        pcapManager.stopPcapFileCreating();
    }
}
