
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

package com.bytechef.hermes.configuration.remote.web.rest.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/internal/workflow-service")
public class WorkflowServiceController {

    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowServiceController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflow/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Workflow> getWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflows",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Workflow>> getWorkflows() {
        return ResponseEntity.ok(workflowService.getWorkflows());
    }
}
