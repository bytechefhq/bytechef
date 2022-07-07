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

package com.bytechef.atlas.service.workflow;

import com.bytechef.atlas.repository.workflow.WorkflowRepository;
import com.bytechef.atlas.workflow.domain.Workflow;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    public WorkflowService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Transactional(readOnly = true)
    public Workflow getWorkflow(String id) {
        return workflowRepository.findOne(id);
    }

    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows() {
        return workflowRepository.findAll();
    }

    public Workflow create(String content, String format) {
        return workflowRepository.create(content, format);
    }

    public Workflow update(String id, String content, String format) {
        return workflowRepository.update(id, content, format);
    }
}
