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

package org.qubership.automation.itf.ui.aspects;

import java.util.concurrent.Callable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.slf4j.LoggerFactory;

@Aspect
public class TransactionAspect implements MethodInterceptor {

    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        return TxExecutor.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    return methodInvocation.proceed();
                } catch (Throwable throwable) {
                    LoggerFactory.getLogger(TransactionAspect.class).error("Failed to execute Tx method", throwable);
                    if (throwable instanceof Exception) {
                        throw (Exception) throwable;
                    } else {
                        throw new Exception(throwable);
                    }
                }
            }
        });
    }
}
