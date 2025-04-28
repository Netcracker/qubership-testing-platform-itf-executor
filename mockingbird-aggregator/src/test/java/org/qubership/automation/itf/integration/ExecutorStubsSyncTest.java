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

package org.qubership.automation.itf.integration;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = {ExecutorIntegrationConfig.class, ExecutorToMessageBrokerSender.class})
public class ExecutorStubsSyncTest {
//    @Autowired
//    ExecutorToMessageBrokerSender sender;
//
//    @BeforeClass
//    public static void initializeConfiguration(){
//        System.setProperty("message-broker.url", "tcp://127.0.0.1:61616");
//        System.setProperty("message-broker.executor-stubs-sync.topic", "executor-stubs-sync");
//        System.setProperty("message-broker.eds-update.topic", "eds_update");
//    }
//
//    @Test
//    public void messageSuccessfullySentToTopic() throws InterruptedException {
//        String message = "Ready";
//        sender.sendMessageToExecutorStubsSyncTopic(message);
//
//        System.out.println("Starting listener ..." );
//        Thread.sleep(300*1000);
//        System.out.println("Done.");
//    }
//
//    @Test
//    public void sendMessageToExternalDataStorageUpdateTopic() {
//        System.out.println("Send file info");
//        sender.sendMessageToExternalDataStorageUpdateTopic(new FileInfo(null, "test.txt", "/test", "wsdl-xsd",
//                UUID.randomUUID(), new ByteArrayInputStream("test".getBytes())));
//        System.out.println("Done.");
//    }
}
