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

package org.qubership.automation.itf.ui.controls.eci;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.environments.openapi.dto.ConnectionDto;
import org.qubership.atp.environments.openapi.dto.ConnectionFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.EnvironmentFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.EnvironmentNameViewDto;
import org.qubership.atp.environments.openapi.dto.EnvironmentResDto;
import org.qubership.atp.environments.openapi.dto.ProjectFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.ProjectNameViewDto;
import org.qubership.atp.environments.openapi.dto.SystemFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.SystemNameViewDto;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsConnectionFeignClient;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsEnvironmentFeignClient;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsProjectFeignClient;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsSystemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/eci/api")
public class ECIAPIController {

    @Autowired
    private AtpEnvironmentsEnvironmentFeignClient atpEnvironmentsEnvironmentFeignClient;
    @Autowired
    private AtpEnvironmentsSystemFeignClient atpEnvironmentsSystemFeignClient;
    @Autowired
    private AtpEnvironmentsProjectFeignClient atpEnvironmentsProjectFeignClient;
    @Autowired
    private AtpEnvironmentsConnectionFeignClient atpEnvironmentsConnectionFeignClient;

    /**
     * Get list projects from atp-environment.
     *
     * @return collection {@code {@link List<ProjectNameViewDto>}} with projects.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/projects")
    public List<ProjectNameViewDto> getProjectsShort(@RequestParam("projectUuid") UUID projectUuid) {
        List<ProjectNameViewDto> projectNameViewDtoList
                = atpEnvironmentsProjectFeignClient.getAllShort(false).getBody();
        return projectNameViewDtoList;
    }

    /**
     * Get project from atp-environment.
     *
     * @param ecProjectId environment configurator project id
     * @return project model {@link ProjectFullVer1ViewDto}.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/project")
    public ProjectFullVer1ViewDto getProject(@RequestParam("ecProjectId") UUID ecProjectId,
                                             @RequestParam("projectUuid") UUID projectUuid) {
        ProjectFullVer1ViewDto projectFullVer1ViewDto
                = atpEnvironmentsProjectFeignClient.getProject(ecProjectId, false).getBody();
        return projectFullVer1ViewDto;
    }

    /**
     * Get list environments from atp-environment.
     *
     * @param ecProjectId environment configurator project id
     * @return collection {@code {@link List<EnvironmentNameViewDto>}} with environments.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/environments")
    public List<EnvironmentNameViewDto> getEnvironmentsShort(@RequestParam("ecProjectId") UUID ecProjectId,
                                                             @RequestParam("projectUuid") UUID projectUuid) {
        List<EnvironmentNameViewDto> environmentNameViewDtoList
                = atpEnvironmentsProjectFeignClient.getEnvironmentsShort(ecProjectId).getBody();
        return environmentNameViewDtoList;
    }

    /**
     * Get list environments with full info from atp-environment.
     *
     * @param ecProjectId environment configurator project id
     * @return collection {@code {@link List<EnvironmentResDto>}} with environments.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/environments/full")
    public List<EnvironmentResDto> getEnvironmentsFull(@RequestParam("ecProjectId") UUID ecProjectId,
                                                       @RequestParam("projectUuid") UUID projectUuid) {
        List<EnvironmentResDto> environmentResDtoList
                = atpEnvironmentsProjectFeignClient.getEnvironments(ecProjectId, true).getBody();
        return environmentResDtoList;
    }

    /**
     * Get environment from atp-environment.
     *
     * @param ecEnvId environment object id
     * @return object {@link EnvironmentFullVer1ViewDto}.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/environment")
    public EnvironmentFullVer1ViewDto getEnvironment(@RequestParam("ecEnvId") UUID ecEnvId,
                                                     @RequestParam("projectUuid") UUID projectUuid) {
        EnvironmentFullVer1ViewDto environmentFullVer1ViewDto
                = atpEnvironmentsEnvironmentFeignClient.getEnvironment(ecEnvId, false).getBody();
        return environmentFullVer1ViewDto;
    }

    /**
     * Get environment with full info from atp-environment.
     *
     * @param ecEnvId environment object id
     * @return object {@link EnvironmentFullVer1ViewDto}.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/environment/full")
    public EnvironmentFullVer1ViewDto getEnvironmentFull(@RequestParam("ecEnvId") UUID ecEnvId,
                                                         @RequestParam("projectUuid") UUID projectUuid) {
        EnvironmentFullVer1ViewDto environmentFullVer1ViewDto
                = atpEnvironmentsEnvironmentFeignClient.getEnvironment(ecEnvId, true).getBody();
        return environmentFullVer1ViewDto;
    }

    /**
     * Get list systems from atp-environment.
     *
     * @param ecEnvId environment object id
     * @return collection {@code {@link List<SystemNameViewDto>}} with systems.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/systems")
    public List<SystemNameViewDto> getSystemsShort(@RequestParam("ecEnvId") UUID ecEnvId,
                                                   @RequestParam("projectUuid") UUID projectUuid) {
        List<SystemNameViewDto> systemNameViewDtoList
                = atpEnvironmentsEnvironmentFeignClient.getSystemsShort(ecEnvId).getBody();
        return systemNameViewDtoList;
    }

    /**
     * Get system from atp-environment.
     *
     * @param ecSystemId system object id
     * @return object {@link SystemFullVer1ViewDto}.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/system")
    public SystemFullVer1ViewDto getSystem(@RequestParam("ecSystemId") UUID ecSystemId,
                                           @RequestParam("projectUuid") UUID projectUuid) {
        SystemFullVer1ViewDto systemFullVer1ViewDto
                = atpEnvironmentsSystemFeignClient.getSystem(ecSystemId, false).getBody();
        return systemFullVer1ViewDto;
    }

    /**
     * Get system with full info from atp-environment.
     *
     * @param ecSystemId system object id
     * @return object {@link SystemFullVer1ViewDto}.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/system/full")
    public SystemFullVer1ViewDto getSystemFull(@RequestParam("ecSystemId") UUID ecSystemId,
                                               @RequestParam("projectUuid") UUID projectUuid) {
        SystemFullVer1ViewDto systemFullVer1ViewDto
                = atpEnvironmentsSystemFeignClient.getSystem(ecSystemId, true).getBody();
        return systemFullVer1ViewDto;
    }

    /**
     * Get list connections from atp-environment.
     *
     * @param ecSystemId system object id
     * @return collection {@code {@link List<ConnectionFullVer1ViewDto>}} with connections.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/connections")
    public List<ConnectionFullVer1ViewDto> getConnections(@RequestParam("ecSystemId") UUID ecSystemId,
                                                          @RequestParam("projectUuid") UUID projectUuid)
            throws IOException {
        List<ConnectionFullVer1ViewDto> connectionFullVer1ViewDtoList
                = atpEnvironmentsSystemFeignClient.getSystemConnections(ecSystemId, false).getBody();
        return connectionFullVer1ViewDtoList;
    }

    /**
     * Get connection from atp-environment.
     *
     * @param ecConnectionId connection object id
     * @return object {@link ConnectionDto}.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/connection")
    public ConnectionDto getConnection(@RequestParam("ecConnectionId") UUID ecConnectionId,
                                       @RequestParam("projectUuid") UUID projectUuid) {
        ConnectionDto connectionDto
                = atpEnvironmentsConnectionFeignClient.getConnection(ecConnectionId, false).getBody();
        return connectionDto;
    }
}
