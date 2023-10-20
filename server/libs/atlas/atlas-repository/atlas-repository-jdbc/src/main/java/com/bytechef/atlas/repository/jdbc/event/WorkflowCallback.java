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
import com.bytechef.atlas.workflow.WorkflowResource;
import com.bytechef.commons.uuid.UUIDGenerator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
        workflow = readWorkflowContent(workflow);

        return workflow;
    }

    @Override
    public Workflow onBeforeConvert(Workflow workflow) {
        // TODO check why Auditing does not populate auditing fields
        if (workflow.isNew()) {
            workflow.setCreatedBy("system");
            workflow.setCreatedDate(LocalDateTime.now());
            workflow.setId(UUIDGenerator.generate());
        }

        workflow.setLastModifiedBy("system");
        workflow.setLastModifiedDate(LocalDateTime.now());

        return workflow;
    }

    private Workflow readWorkflowContent(Workflow workflow) {
        return workflowMapper.readValue(new WorkflowResource(
                workflow.getId(),
                new ByteArrayResource(workflow.getContent().getBytes(StandardCharsets.UTF_8)),
                workflow.getFormat()));
    }
}
