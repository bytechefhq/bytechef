
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
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class ProjectInstanceServiceController {

    private final ProjectInstanceService projectInstanceService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceServiceController(ProjectInstanceService projectInstanceService) {
        this.projectInstanceService = projectInstanceService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/project-instance-service/fetch-project-instance/{jobId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectInstance> fetchJobProjectInstance(@PathVariable long jobId) {
        return projectInstanceService.fetchJobProjectInstance(jobId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-instance-service/update-enabled/{id}/{enabled}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Void> updateEnabled(@PathVariable long id, @PathVariable boolean enabled) {
        projectInstanceService.updateEnabled(id, enabled);

        return ResponseEntity.noContent()
            .build();
    }
}
