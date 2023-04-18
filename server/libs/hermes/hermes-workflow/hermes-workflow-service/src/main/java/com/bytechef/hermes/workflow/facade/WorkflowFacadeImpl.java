
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
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.connection.WorkflowConnection;
import com.bytechef.hermes.workflow.dto.WorkflowDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowFacadeImpl implements WorkflowFacade {

    private final WorkflowService workflowService;

    public WorkflowFacadeImpl(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowDTO create(String definition, Format format, SourceType sourceType) {
        Workflow workflow = workflowService.create(definition, format, sourceType);

        return new WorkflowDTO(getConnections(workflow), workflow);
    }

    @Override
    public WorkflowDTO getWorkflow(String id) {
        Workflow workflow = workflowService.getWorkflow(id);

        return new WorkflowDTO(getConnections(workflow), workflow);
    }

    @Override
    public List<WorkflowDTO> getWorkflows() {
        return CollectionUtils.map(
            workflowService.getWorkflows(),
            workflow -> new WorkflowDTO(getConnections(workflow), workflow));
    }

    @Override
    public WorkflowDTO update(String id, String definition) {
        Workflow workflow = workflowService.update(id, definition);

        return new WorkflowDTO(getConnections(workflow), workflow);
    }

    @Override
    public List<WorkflowDTO> getWorkflows(List<String> workflowIds) {
        return CollectionUtils.map(
            workflowService.getWorkflows(),
            workflow -> new WorkflowDTO(getConnections(workflow), workflow));
    }

    private List<WorkflowConnection> getConnections(Workflow workflow) {
        List<WorkflowConnection> connections = new ArrayList<>();

        for (WorkflowTask workflowTask : workflow.getTasks()) {
            workflowTask
                .fetchExtension(WorkflowConnection.class)
                .ifPresent(connections::add);
        }

        return connections;
    }
}
