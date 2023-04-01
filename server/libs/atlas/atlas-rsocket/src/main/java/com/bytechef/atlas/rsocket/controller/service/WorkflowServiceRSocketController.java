
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

package com.bytechef.atlas.rsocket.controller.service;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class WorkflowServiceRSocketController {

    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowServiceRSocketController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @MessageMapping("createWorkflow")
    public Mono<Workflow> createWorkflow(Workflow workflow) {
        return Mono.create(sink -> sink.success(
            workflowService.create(workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType())));
    }

    @MessageMapping("getWorkflow")
    public Mono<Workflow> getWorkflow(String id) {
        return Mono.create(sink -> sink.success(workflowService.getWorkflow(id)));
    }

    @MessageMapping("getWorkflows")
    public Mono<List<Workflow>> getWorkflows() {
        return Mono.create(sink -> sink.success(workflowService.getWorkflows()));
    }

    @MessageMapping("updateWorkflow")
    public Mono<Workflow> updateWorkflow(Workflow workflow) {
        return Mono.create(sink -> sink.success(workflowService.update(workflow.getId(), workflow.getDefinition())));
    }
}
