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

package org.qubership.automation.itf.report.engine;

public class ATPReportEngineTest {
    /*
    @Before
    public void setUp() throws Exception {
        ExecutorServiceProvider serviceProvider = mock(ExecutorServiceProvider.class);
        ExecutorServiceProviderFactory.init(serviceProvider);
        when(serviceProvider.requestForBackgroundJob()).thenReturn(Executors.newSingleThreadExecutor());
    }

    @Ignore
    @Test
    public void testLogRecordIsCalledAfterAddingNewReportItem() throws Exception {
        ATPReportEngine reportEngine = new ATPReportEngine();
        reportEngine.start();
        ATPReportItem reportItem = mock(ATPReportItem.class);
        AbstractInstance instance = mock(AbstractInstance.class);
        InstanceContext value = new InstanceContext();
        TcContext tc = new TcContext();
        tc.setID("1");
        value.setTC(tc);
        when(instance.getContext()).thenReturn(value);
        when(reportItem.getObject()).thenReturn(instance);
        reportEngine.logItem(reportItem, true);
        tc.setID("2");
        reportEngine.logItem(reportItem, true);
        Thread.sleep(100); //Let the logger a little bit time to submit and execute thread
        verify(reportItem, times(2)).logRecord();
    }*/
}
