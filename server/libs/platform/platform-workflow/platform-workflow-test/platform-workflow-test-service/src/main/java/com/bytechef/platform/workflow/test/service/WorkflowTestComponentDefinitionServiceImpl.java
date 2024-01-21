/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.workflow.test.service;

import com.bytechef.platform.workflow.test.domain.WorkflowTestComponentDefinition;
import com.bytechef.platform.workflow.test.repository.WorkflowTestComponentDefinitionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkflowTestComponentDefinitionServiceImpl implements WorkflowTestComponentDefinitionService {

    private final WorkflowTestComponentDefinitionRepository workflowTestComponentDefinitionRepository;

    public WorkflowTestComponentDefinitionServiceImpl(
        WorkflowTestComponentDefinitionRepository workflowTestComponentDefinitionRepository) {

        this.workflowTestComponentDefinitionRepository = workflowTestComponentDefinitionRepository;
    }

    @Override
    public Optional<WorkflowTestComponentDefinition> fetchWorkflowTestComponentDefinition(
        String workflowId, String triggerName) {

        return workflowTestComponentDefinitionRepository.findByWorkflowIdAndOperationName(workflowId, triggerName);
    }
}
