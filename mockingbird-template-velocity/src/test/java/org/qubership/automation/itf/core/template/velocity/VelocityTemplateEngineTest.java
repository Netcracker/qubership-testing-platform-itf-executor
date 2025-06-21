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

package org.qubership.automation.itf.core.template.velocity;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.exception.VelocityException;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.qubership.automation.itf.core.hibernate.ManagerFactory;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.counter.Counter;
import org.qubership.automation.itf.core.model.counter.CounterImpl;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testng.Assert;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:*template-velocity-test-context.xml"})
public class VelocityTemplateEngineTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private VelocityTemplateEngine engine;

    @Before
    public void setUp() {
        engine = new VelocityTemplateEngine();
    }

    @Test
    public void testTools() {
        String sourceString = "$date";
        Server owner = mock(Server.class);
        String process = engine.process(owner, sourceString, InstanceContext.from(null, null));
        System.out.println(process);
    }

    @Test
    public void process() {
        String sourceString = "test $tc.aaa $tc.ccc.ddd $tc.fff[0]";
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        context.create("ccc");
        context.put("ccc.ddd", "eee");
        context.create("fff", true);
        context.put("fff[0]", "jjj");
        long start = System.currentTimeMillis();
        Server owner = mock(Server.class);
        String process = engine.process(owner, sourceString, InstanceContext.from(context, null));
        long finish = System.currentTimeMillis();
        System.out.println(process);
        System.out.println(finish - start);
        Assert.assertEquals(process, "test bbb eee jjj");
    }

    public void throwTest() {
        String sourceString = "aaa $tc.bbb.ccc $tc.aaa bbb";
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        String process = engine.process(mock(Server.class), sourceString, InstanceContext.from(context, null));
        System.out.println(process);
    }

    @Test
    public void testEscapeXml() {
        String sourceString = "test #escape_xml('<xml>') #escape_xml($tc.aaa)";
        TcContext context = new TcContext();
        context.put("aaa", "<bbb>");
        String process = engine.process(mock(Server.class), sourceString, InstanceContext.from(context, null));
        System.out.println(process);
    }

    @Test
    public void testGenerateId() {
        String sourceString = "#generateUUID()";
        String process = engine.process(mock(Server.class), sourceString, InstanceContext.from(null, null));
        Assert.assertNotEquals(process, sourceString);
        System.out.println(process);
    }

    @Test
    public void testLoadPartThrowsExceptionIfTemplateNotFound() {
        expectedException.expect(VelocityException.class);
        expectedException.expectMessage(StringContains.containsString("template not found"));
        ObjectManager<SystemTemplate> manager = configureObjectManager().getManager(SystemTemplate.class);
        answer(manager, "secondTemplate", invocation -> Collections.emptyList(), invocation -> {
            throw new IllegalArgumentException();
        });
        String source = "root #load_part(\"secondTemplate\")";
        SystemTemplate firstTemplate = mock(SystemTemplate.class);
        when(firstTemplate.toString()).thenReturn("WMS TK Template");
        when(firstTemplate.getName()).thenReturn("firstTemplate");
        engine.process(firstTemplate, source, InstanceContext.from(null, null));
    }

    @Test
    public void testLoadPartInChildReturnsValue() {
        String source = "source #load_part(\"firstTemplate\")";
        String firstTemplateSource = "first #load_part(\"secondTemplate\")";
        String secondTemplateSource = "second";
        Template firstTemplate = mock(SystemTemplate.class);
        when(firstTemplate.getText()).thenReturn(firstTemplateSource);
        Template secondTemplate = mock(SystemTemplate.class);
        when(secondTemplate.getText()).thenReturn(secondTemplateSource);
        ObjectManager<SystemTemplate> manager = configureObjectManager().getManager(SystemTemplate.class);
        answer(manager, "firstTemplate", invocation -> Collections.singletonList(firstTemplate), invocation -> null);
        answer(manager, "secondTemplate", invocation -> Collections.emptyList(), invocation -> secondTemplate);
        String process = engine.process(firstTemplate, source, InstanceContext.from(null, null));
        assertEquals("source first second", process);
    }

    @Test
    public void testNexIndexForNNNFormat() throws Exception {
        ObjectManager<Counter> counterObjectManager = configureObjectManager().getManager(Counter.class);
        Counter counter = mock(Counter.class);
        when(counter.getDate()).thenReturn(Calendar.getInstance().getTime());
        when(counter.getIndex()).thenReturn(2);
        when(counter.getNextIndex()).thenReturn(3);
        when(counter.getOwners()).thenReturn(Sets.newHashSet(new BigInteger(String.valueOf(4)),
                new BigInteger(String.valueOf(5))));
        when(counterObjectManager.getAll()).then(invocation -> Collections.singletonList(counter));
        CounterImpl counterImpl = new CounterImpl();
        doNothing().when(counterObjectManager).store(counterImpl);
        when(counterObjectManager.create(null)).then(invocation -> counterImpl);
        String source = "source #next_index(\"NNN\")" + "#set($tc.filename = \"FxP_SMK_CPLExport_20170912_\"+ "
                + "\"#next_index('NNN')\" +\".csv.gz\")";
        Map<String, Storable> map = Maps.newHashMap();
        Template template = mock(SystemTemplate.class);
        map.put(TemplateEngine.OWNER, template);
        when(template.getID()).thenReturn(new BigInteger(String.valueOf(1)));
        Operation operation = mock(Operation.class);
        map.put(TemplateEngine.OPERATION, operation);
        when(operation.getID()).thenReturn(new BigInteger(String.valueOf(2)));
        Environment environment = mock(Environment.class);
        map.put(TemplateEngine.ENVIRONMENT, environment);
        when(environment.getID()).thenReturn(new BigInteger(String.valueOf(3)));
        for (int i = 1; i < 1000; i++) {
            TcContext tc = new TcContext();
            String process = engine.process(map, source, InstanceContext.from(tc, null));
            assertEquals("source " + prepareIndex(i), process);
            System.out.println(process);
        }
    }

    @Test
    public void testAddDate() {
        String sourceString = "#add_date($tc.date, \"1d\", \"2h\", \"5m\", \"2d\")";
        TcContext context = new TcContext();
        context.put("date", "2018-08-28T04:53:46");
        String processed = engine.process(mock(Server.class), sourceString, InstanceContext.from(context, null));
        Assert.assertEquals(processed, "2018-08-31T06:58:46");
        System.out.println(processed);
    }

    @Test
    public void testMathTool() {
        String sourceString = "$math.random(100,999)";
        TcContext context = new TcContext();
        Map<String, Storable> map = Maps.newHashMap();
        String processed = engine.process(map, sourceString, InstanceContext.from(context, null));
        Assert.assertNotEquals(processed, sourceString);
    }

    @Test
    public void testDateTool() {
        String sourceString = "#set($startDate = $date.format('yyyy-MM-dd''T''HH:mm:ss', $date))\n$startDate";
        TcContext context = new TcContext();
        Map<String, Storable> map = Maps.newHashMap();
        String processed = engine.process(map, sourceString, InstanceContext.from(context, null));
        Assert.assertFalse(processed.contains("$date"));
        System.out.println(processed);
    }

    @Test
    public void testEscTool() {
        String htmlString = "Some String with html/xml tags: <customer><name>Alex</name><id>54321</id></customer>";
        String velocityString = "$esc.html(\"" + htmlString + "\")";
        TcContext context = new TcContext();
        Map<String, Storable> map = Maps.newHashMap();
        String processed = engine.process(map, velocityString, InstanceContext.from(context, null));
        Assert.assertEquals(processed, StringEscapeUtils.escapeHtml(htmlString));
    }

    @Test
    public void testNumberTool() {
        String velocityString = "#set($num = 55.666)\n$number.format(\"#0000\", $num)";
        TcContext context = new TcContext();
        Map<String, Storable> map = Maps.newHashMap();
        String processed = engine.process(map, velocityString, InstanceContext.from(context, null));
        Assert.assertEquals(processed, "0056");
    }

    private String prepareIndex(int i) {
        switch (Integer.toString(i).length()) {
            case 1:
                return "00" + i;
            case 2:
                return "0" + i;
            default:
                return Integer.toString(i);
        }
    }

    private ManagerFactory configureObjectManager() {
        ManagerFactory managerFactory = mock(ManagerFactory.class);
        ObjectManager<SystemTemplate> templateOM = mock(ObjectManager.class);
        ObjectManager<Counter> counterOM = mock(ObjectManager.class);
        ObjectManager<CounterImpl> counterImOM = mock(ObjectManager.class);
        when(managerFactory.getManager(SystemTemplate.class)).thenReturn(templateOM);
        when(managerFactory.getManager(Counter.class)).thenReturn(counterOM);
        when(managerFactory.getManager(CounterImpl.class)).thenReturn(counterImOM);
        CoreObjectManager.getInstance().setManagerFactory(managerFactory);
        return managerFactory;
    }

    private void answer(ObjectManager<SystemTemplate> manager, String identifier, Answer byName, Answer byId) {
        when(manager.getByName(eq(identifier))).then(byName);
        when(manager.getById(eq(identifier))).then(byId);
    }
}
