
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

package com.bytechef.atlas.repository.jdbc.event;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowResource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.relational.core.mapping.event.AfterConvertCallback;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Order(1)
@Component
public class WorkflowCallback implements AfterConvertCallback<Workflow>, BeforeConvertCallback<Workflow> {

    private final WorkflowMapper workflowMapper;

    public WorkflowCallback(WorkflowMapper workflowMapper) {
        this.workflowMapper = workflowMapper;
    }

    @Override
    public Workflow onAfterConvert(Workflow workflow) {
        return readWorkflowDefinition(workflow);
    }

    @Override
    public Workflow onBeforeConvert(Workflow workflow) {
        if (workflow.isNew()) {
            UUID uuid = UUID.randomUUID();

            workflow.setId(uuid.toString());
        }

        return workflow;
    }

    private Workflow readWorkflowDefinition(Workflow workflow) {
        String definition = workflow.getDefinition();

        Workflow newWorkflow = workflowMapper.readValue(
            new WorkflowResource(
                workflow.getId(),
                new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)),
                workflow.getFormat()));

        newWorkflow.setCreatedBy(workflow.getCreatedBy());
        newWorkflow.setCreatedDate(workflow.getCreatedDate());
        newWorkflow.setLastModifiedBy(workflow.getLastModifiedBy());
        newWorkflow.setLastModifiedDate(workflow.getLastModifiedDate());
        newWorkflow.setVersion(workflow.getVersion());

        return newWorkflow;
    }
}
