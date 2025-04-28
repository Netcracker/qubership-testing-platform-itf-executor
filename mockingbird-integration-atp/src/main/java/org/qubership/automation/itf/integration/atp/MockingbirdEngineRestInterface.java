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

package org.qubership.automation.itf.integration.atp;

import static org.qubership.automation.itf.integration.atp.util.DefinitionBuilder.createAdvancedValueListWithCallChains;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.integration.atp.model.AdvancedValue;
import org.qubership.automation.itf.integration.atp.model.CoolDownRequest;
import org.qubership.automation.itf.integration.atp.model.CoolDownResponse;
import org.qubership.automation.itf.integration.atp.model.ExecuteStepResponse;
import org.qubership.automation.itf.integration.atp.model.GetAdditionStepInfoResponse;
import org.qubership.automation.itf.integration.atp.model.GetAdditionalStepInfoRequest;
import org.qubership.automation.itf.integration.atp.model.GetDefinitionsResponse;
import org.qubership.automation.itf.integration.atp.model.PingResponse;
import org.qubership.automation.itf.integration.atp.model.WarmUpResponse;
import org.qubership.automation.itf.integration.atp.model.ram2.Ram2ExecuteStepRequest;
import org.qubership.automation.itf.integration.atp.model.ram2.Ram2WarmUpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Tags({
        @Tag(name = "ITF REST API queries.", description = "ITF REST API to query ITF."),
        @Tag(name = "ITF REST API commands.", description = "ITF REST API to control ITF.")
})
@Transactional(readOnly = true)
@RestController
public class MockingbirdEngineRestInterface {

    private MockingbirdEngineService mockingbirdEngineService;

    @Autowired
    public MockingbirdEngineRestInterface(MockingbirdEngineService mockingbirdEngineService) {
        this.mockingbirdEngineService = mockingbirdEngineService;
    }

    @GetMapping(value = "/ping")
    @Operation(summary = "Ping", description = "Ping ITF", tags = {"ITF REST API queries."})
    public PingResponse ping() {
        return mockingbirdEngineService.ping();
    }

    @GetMapping(value = "/getDefinitions")
    @Operation(summary = "GetDefinitions", description = "Retrieve ITF Definitions", tags = {"ITF REST API queries."})
    public GetDefinitionsResponse getDefinitions(@RequestParam(value = "projectId") UUID projectUuid) {
        BigInteger projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class,
                SearchManager.class).getEntityInternalIdByUuid(projectUuid);
        return mockingbirdEngineService.getDefinitions(projectId);

    }

    @GetMapping(value = "/getCallchains")
    @Operation(summary = "GetCallChains", description = "Retrieve CallChains", tags = {"ITF REST API queries."})
    public List<AdvancedValue> getCallChains(@RequestParam(value = "projectId") UUID projectUuid) {
        BigInteger projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class,
                SearchManager.class).getEntityInternalIdByUuid(projectUuid);
        return createAdvancedValueListWithCallChains(projectId);
    }

    @PostMapping(value = "/getAdditionalStepInfo")
    @Operation(summary = "GetAdditionStepInfo", description = "Get additional step info",
            tags = {"ITF REST API queries."})
    public GetAdditionStepInfoResponse getAdditionStepInfo(
            @RequestBody GetAdditionalStepInfoRequest getAdditionalStepInfoRequest) {
        return mockingbirdEngineService.getAdditionStepInfo(getAdditionalStepInfoRequest);
    }

    @PostMapping(value = "/executeStep")
    @Operation(summary = "ExecuteStep", description = "Execute Step", tags = {"ITF REST API commands."})
    public ExecuteStepResponse executeStep(@RequestBody Ram2ExecuteStepRequest ram2ExecuteStepRequest) {
        return mockingbirdEngineService.executeStep(ram2ExecuteStepRequest);
    }

    @PostMapping(value = "/warmUp")
    @Operation(summary = "WarmUp", description = "Warm up", tags = {"ITF REST API commands."})
    public WarmUpResponse warmUp(@RequestBody Ram2WarmUpRequest ram2WarmUpRequest) {
        return mockingbirdEngineService.warmUp(ram2WarmUpRequest);
    }

    @PostMapping(value = "/coolDown")
    @Operation(summary = "CoolDown", description = "Cooldown", tags = {"ITF REST API commands."})
    public CoolDownResponse coolDown(@RequestBody CoolDownRequest coolDownRequest) {
        return mockingbirdEngineService.coolDown(coolDownRequest);
    }
}
