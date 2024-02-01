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

package com.bytechef.platform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.definition.WorkflowNodeType;
import com.bytechef.platform.component.registry.domain.ObjectProperty;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.registry.util.SchemaUtils;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkflowNodeTestOutputServiceImpl implements WorkflowNodeTestOutputService {

    private final WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    public WorkflowNodeTestOutputServiceImpl(
        WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository) {

        this.workflowNodeTestOutputRepository = workflowNodeTestOutputRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowNodeTestOutput> fetchWorkflowTestNodeOutput(String workflowId, String workflowNodeName) {
        return workflowNodeTestOutputRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName);
    }

    @Override
    public WorkflowNodeTestOutput save(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType, Map<String, ?> sampleOutput) {

        ObjectProperty outputSchema = Property.toProperty(
            (com.bytechef.component.definition.Property) SchemaUtils.getOutputSchema(
                sampleOutput, new PropertyFactory(sampleOutput)));

        return save(workflowId, workflowNodeName, workflowNodeType, outputSchema, sampleOutput);
    }

    @Override
    public WorkflowNodeTestOutput save(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType, Output output) {

        return save(
            workflowId, workflowNodeName, workflowNodeType, output.getOutputSchema(), output.getSampleOutput());
    }

    private WorkflowNodeTestOutput save(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType,
        Property outputSchema, Map<String, ?> sampleOutput) {

        WorkflowNodeTestOutput workflowNodeTestOutput = OptionalUtils.orElse(
            workflowNodeTestOutputRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName),
            new WorkflowNodeTestOutput());

        workflowNodeTestOutput.setComponentName(workflowNodeType.componentName());
        workflowNodeTestOutput.setComponentOperationName(workflowNodeType.componentOperationName());
        workflowNodeTestOutput.setComponentVersion(workflowNodeType.componentVersion());
        workflowNodeTestOutput.setOutputSchema(outputSchema);
        workflowNodeTestOutput.setSampleOutput(sampleOutput);
        workflowNodeTestOutput.setWorkflowId(workflowId);
        workflowNodeTestOutput.setWorkflowNodeName(workflowNodeName);

        return workflowNodeTestOutputRepository.save(workflowNodeTestOutput);
    }
}
