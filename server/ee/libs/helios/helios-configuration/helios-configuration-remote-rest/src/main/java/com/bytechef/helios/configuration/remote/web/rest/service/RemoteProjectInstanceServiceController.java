
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.helios.configuration.remote.web.rest.service;

import com.bytechef.helios.configuration.domain.ProjectInstance;
import com.bytechef.helios.configuration.service.RemoteProjectInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/project-instance-service")
public class RemoteProjectInstanceServiceController {

    private final RemoteProjectInstanceService projectInstanceService;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceServiceController(RemoteProjectInstanceService projectInstanceService) {
        this.projectInstanceService = projectInstanceService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-workflow-project-instance/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectInstance> fetchWorkflowProjectInstance(@PathVariable String workflowId) {
        return projectInstanceService.fetchWorkflowProjectInstance(workflowId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent()
                .build());
    }
}
