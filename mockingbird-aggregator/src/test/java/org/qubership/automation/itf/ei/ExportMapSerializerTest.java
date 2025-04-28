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

package org.qubership.automation.itf.ei;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.server.ServerHB;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.constants.TriggerState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ExportMapSerializerTest {

    private final String entityER = "{\"id\":null,\"name\":\"SQL Environment\",\"parent\":9165840781811397936," +
            "\"prefix\":null,"
            + "\"description\":null,\"storableProp\":null,\"labels\":[],\"ecId\":null,\"ecProjectId\":null,"
            + "\"ecLabel\":null,\"outbound\":{\"9166484358111414033\":9166484358111414055},"
            + "\"inbound\":{\"9166484358111414034\":9166484358111414066},"
            + "\"reportCollectors\":[],\"environmentState\":\"Empty\",\"projectId\":9165840781811397933,"
            + "\"extendsParameters\":null,\"version\":null}";

    @Test
    public void testSerialization() throws JsonProcessingException {
        Environment environment = new Environment();
        environment.setProjectId(new BigInteger("9165840781811397933"));
        environment.setEnvironmentState(TriggerState.EMPTY);
        environment.setName("SQL Environment");

        EnvFolder envFolder = new EnvFolder();
        envFolder.setID(new BigInteger("9165840781811397936"));
        environment.setParent(envFolder);

        EnvFolder rootFolder = new EnvFolder();
        envFolder.setParent(rootFolder);

        System systemOutbound = new System();
        systemOutbound.setID(new BigInteger("9166484358111414033"));
        ServerHB serverHbOutbound = new ServerHB();
        serverHbOutbound.setID(new BigInteger("9166484358111414055"));

        Map<System, Server> outbound = new HashMap<>();
        outbound.put(systemOutbound, serverHbOutbound);
        environment.fillOutbound(outbound);

        System systemInbound = new System();
        systemInbound.setID(new BigInteger("9166484358111414034"));
        ServerHB serverHbInbound = new ServerHB();
        serverHbInbound.setID(new BigInteger("9166484358111414066"));

        Map<System, Server> inbound = new HashMap<>();
        inbound.put(systemInbound, serverHbInbound);
        environment.fillInbound(inbound);

        ObjectMapper objectMapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String entityAR = objectMapper.writeValueAsString(environment);
        Assert.assertEquals(entityER, entityAR);
    }

}
