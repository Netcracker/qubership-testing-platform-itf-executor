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

package org.qubership.automation.itf.integration.users;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.entities.Project;
import org.qubership.automation.itf.integration.converter.DtoConvertService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtpUsersService {

    private final UsersProjectFeignClient usersProjectFeignClient;
    private final DtoConvertService dtoConvertService;

    public List<Project> getAllProjects() {
        return dtoConvertService.convertList(usersProjectFeignClient.getAllProjects().getBody(), Project.class);
    }

    public Project getProjectUsersByProjectId(UUID projectId) {
        return dtoConvertService.convert(
                usersProjectFeignClient.getProjectUsersByProjectId(projectId).getBody(), Project.class);
    }
}
