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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.integration.atp.exector.AtpCallchainExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CallchainRunner implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(CallchainRunner.class);

    private CallchainRunInfo callchainRunInfo;
    private TestRunInfo testRunInfo;
    private AtomicBoolean isFailed;
    private AtpCallchainExecutor atpCallchainExecutor;

    @Autowired
    public CallchainRunner(AtpCallchainExecutor atpCallchainExecutor) {
        this.atpCallchainExecutor = atpCallchainExecutor;
    }

    public void fillRunInfo(CallchainRunInfo callchainRunInfo, TestRunInfo testRunInfo, AtomicBoolean isFailed) {
        this.callchainRunInfo = callchainRunInfo;
        this.testRunInfo = testRunInfo;
        this.isFailed = isFailed;
    }

    @Override
    public void run() {
        TcContext result;
        try {
            result = executeCallChain();
        } catch (Exception e) {
            String title = String.format("Execution of '%s' callchain from ATP is failed.",
                    callchainRunInfo.getCallChain().getName());
            testRunInfo.reportError("Errors", title, StringUtils.EMPTY, e);
            LOGGER.error(title, e);
            result = new TcContext();
            result.setStatus(Status.FAILED);
            isFailed.set(true);
        }
        callchainRunInfo.setTcContext(result);
    }

    private TcContext executeCallChain() throws Exception {
        TcContext context = atpCallchainExecutor.execute(callchainRunInfo, testRunInfo);
        if (context == null) {
            return new TcContext();
        }
        switch (context.getStatus()) {
            case FAILED:
            case FAILED_BY_TIMEOUT:
            case STOPPED:
                isFailed.set(true);
                break;
            default:
                isFailed.set(false);
        }
        context.remove("dataset");
        return context;
    }
}
