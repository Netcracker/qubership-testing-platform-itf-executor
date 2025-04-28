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

package org.qubership.automation.itf.executor.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.qubership.automation.itf.core.util.constants.Status.IN_PROGRESS;
import static org.qubership.automation.itf.core.util.constants.Status.PASSED;
import static org.qubership.automation.itf.core.util.constants.Status.STOPPED;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.automation.itf.core.instance.situation.SituationExecutorService;
import org.qubership.automation.itf.core.instance.testcase.chain.CallChainExecutorService;
import org.qubership.automation.itf.core.instance.testcase.execution.ExecutionProcessManagerService;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TCContextService.class, CallChainExecutorService.class,
        SituationExecutorService.class, ExecutionProcessManagerService.class, ExecutionServices.class})
public class TCContextServiceTest {

    TcContext tcContext;

    @Before
    public void createTCContext() {
        tcContext = new TcContext();
        tcContext.setID(new BigInteger("11111111111111111111111111111111"));
    }

    @Test
    public void startTest() {
        ExecutionServices.getTCContextService().start(tcContext);
        assertEquals(IN_PROGRESS, tcContext.getStatus());
    }

    @Test
    public void startWithWrongStatusTest() {
        ExecutionServices.getTCContextService().start(tcContext);
        tcContext.setStatus(PASSED);
        assertNotEquals(IN_PROGRESS, tcContext.getStatus());
    }

    @Test
    public void stopTest() {
        ExecutionServices.getTCContextService().stop(tcContext);
        assertEquals(STOPPED, tcContext.getStatus());
        assertNotEquals("", tcContext.getEndTime().toString());
    }

    @Test
    public void finishTest() {
        configureObjectManager();
        tcContext.setStatus(IN_PROGRESS);
        ExecutionServices.getTCContextService().finish(tcContext);
        assertEquals(PASSED, tcContext.getStatus());
        assertNotEquals("", tcContext.getEndTime().toString());
    }

    private void configureObjectManager() {
//        ManagerFactory managerFactory = mock(ManagerFactory.class);
//        ObjectManager<TcContext> tcContextOM = mock(TcContextObjectManager.class);
//        when(managerFactory.getManager(TcContext.class)).thenReturn(tcContextOM);
//        CoreObjectManager.setManagerFactory(managerFactory);
    }

    @Test
    public void setMessageParameterTest() {
        Map<String, MessageParameter> messageParameters = createMessageParameters();
        ExecutionServices.getTCContextService().setMessageParameters(tcContext, messageParameters);
        assertTrue(tcContext.containsKey("saved"));
        JsonContext saved = (JsonContext) tcContext.get("saved");
        assertTrue(saved.containsKey("111"));
    }

    private Map<String, MessageParameter> createMessageParameters() {
        MessageParameter.Builder build = MessageParameter.build("111", new SystemParsingRule());
        MessageParameter messageParameter = build.get();
        messageParameter.setAutosave(true);
        List<String> stringList = new ArrayList<>();
        stringList.add("firstMessageParameter");
        messageParameter.setMultipleValue(stringList);
        messageParameter.setMultiple(true);
        Map<String, MessageParameter> map = new HashMap<>();
        map.put("parameters", messageParameter);
        return map;
    }
}
