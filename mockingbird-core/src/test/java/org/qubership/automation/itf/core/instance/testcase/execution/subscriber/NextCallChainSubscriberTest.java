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

package org.qubership.automation.itf.core.instance.testcase.execution.subscriber;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.util.iterator.CallChainStepIterator;

public class NextCallChainSubscriberTest {

    private NextCallChainSubscriber subscriber;

    private CallChainInstance callChainInstance;

    @Before
    public void before() {
        callChainInstance = mock(CallChainInstance.class);
        when(callChainInstance.getID()).thenReturn(1);
        when(this.callChainInstance.iterator()).thenReturn(mock(CallChainStepIterator.class));
        when(this.callChainInstance.iterator().hasNext()).thenReturn(true);
        when(this.callChainInstance.iterator().next()).thenReturn(getStepInstance());
        NextCallChainEvent event = new NextCallChainEvent(null, this.callChainInstance);
        event.setID("1");
        subscriber = new NextCallChainSubscriber(event);
    }

    @Test
    public void synchronizedTest() {
        System.out.println("Start test");
        Thread pauseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    System.out.println("Paused");
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                NextCallChainEvent.Pause pause = new NextCallChainEvent.Pause(null, callChainInstance);
                subscriber.pause(pause);
            }
        });
        pauseThread.start();
        Thread resumeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    System.out.println("Resume");
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                NextCallChainEvent.Resume resume = new NextCallChainEvent.Resume(null, callChainInstance);
                subscriber.resume(resume);
            }
        });
        resumeThread.start();
        for (int i = 0; i < 5; i++) {
            NextCallChainEvent event = new NextCallChainEvent(null, callChainInstance);
            event.setID("1");
            subscriber.handle(event);
        }
    }

    public StepInstance getStepInstance() {
        StepInstance stepInstance = new StepInstance();
        SituationStep step = new SituationStep();
        step.setManual(false);
        step.setDelay(2000);
        step.setUnit(TimeUnit.MILLISECONDS.toString());
        stepInstance.setStepId((BigInteger) step.getID());
        return stepInstance;
    }
}
