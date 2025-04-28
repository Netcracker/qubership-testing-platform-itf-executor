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

package org.qubership.automation.itf.integration.atp.util;

import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;

public class CallchainRunInfo {

    private CallChain callChain;
    private IDataSet dataset;
    private TcContext tcContext;

    public CallchainRunInfo(CallChain callChain, IDataSet dataset) {
        this.callChain = callChain;
        this.dataset = dataset;
    }

    public CallChain getCallChain() {
        return callChain;
    }

    public IDataSet getDataset() {
        return dataset;
    }

    public void setDataset(IDataSet dataset) {
        this.dataset = dataset;
    }

    public TcContext getTcContext() {
        return tcContext;
    }

    public void setTcContext(TcContext tcContext) {
        this.tcContext = tcContext;
    }
}
