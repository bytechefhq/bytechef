
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

package com.bytechef.hermes.workflow.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.domain.Workflow.Format;
import com.bytechef.atlas.domain.Workflow.SourceType;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.hermes.connection.WorkflowConnection;
import com.bytechef.hermes.workflow.WorkflowDTO;
import com.bytechef.hermes.workflow.facade.WorkflowFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowFacadeImpl implements WorkflowFacade {

    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowFacadeImpl(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowDTO create(String definition, Format format, SourceType sourceType) {
        Workflow workflow = workflowService.create(definition, format, sourceType);

        return new WorkflowDTO(WorkflowConnection.of(workflow), workflow);
    }

    @Override
    public WorkflowDTO getWorkflow(String id) {
        Workflow workflow = workflowService.getWorkflow(id);

        return new WorkflowDTO(WorkflowConnection.of(workflow), workflow);
    }

    @Override
    public List<WorkflowDTO> getWorkflows() {
        return workflowService.getWorkflows()
            .stream()
            .map(workflow -> new WorkflowDTO(WorkflowConnection.of(workflow), workflow))
            .toList();
    }

    @Override
    public List<WorkflowDTO> getWorkflows(List<String> workflowIds) {
        return workflowService.getWorkflows()
            .stream()
            .map(workflow -> new WorkflowDTO(WorkflowConnection.of(workflow), workflow))
            .toList();
    }

    @Override
    public WorkflowDTO update(String id, String definition) {
        Workflow workflow = workflowService.update(id, definition);

        return new WorkflowDTO(WorkflowConnection.of(workflow), workflow);
    }
}
