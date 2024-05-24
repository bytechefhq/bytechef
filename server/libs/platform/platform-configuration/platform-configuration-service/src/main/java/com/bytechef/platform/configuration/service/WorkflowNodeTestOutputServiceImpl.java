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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.registry.util.SchemaUtils;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
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
    public void deleteWorkflowNodeTestOutput(String workflowId, String workflowNodeName) {
        workflowNodeTestOutputRepository
            .findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName)
            .ifPresent(workflowNodeTestOutputRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowNodeTestOutput> fetchWorkflowTestNodeOutput(String workflowId, String workflowNodeName) {
        return workflowNodeTestOutputRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName);
    }

    @Override
    public void removeUnusedNodeTestOutputs(Workflow workflow) {
        List<String> workflowTaskNames = workflow
            .getAllTasks()
            .stream()
            .map(WorkflowTask::getName)
            .toList();

        List<String> workflowTriggerNames = WorkflowTrigger
            .of(workflow)
            .stream()
            .map(WorkflowTrigger::getName)
            .toList();

        List<WorkflowNodeTestOutput> workflowNodeTestOutputs = workflowNodeTestOutputRepository
            .findByWorkflowId(Validate.notNull(workflow.getId(), "id"));

        for (WorkflowNodeTestOutput workflowNodeTestOutput : workflowNodeTestOutputs) {
            if (!workflowTaskNames.contains(workflowNodeTestOutput.getWorkflowNodeName()) &&
                !workflowTriggerNames.contains(workflowNodeTestOutput.getWorkflowNodeName())) {

                workflowNodeTestOutputRepository.delete(workflowNodeTestOutput);
            }
        }
    }

    @Override
    public WorkflowNodeTestOutput save(
        @NonNull String workflowId, @NonNull String workflowNodeName, @NonNull WorkflowNodeType workflowNodeType,
        @NonNull Object sampleOutput) {

        Property outputSchema = Property.toProperty(
            (com.bytechef.component.definition.Property) SchemaUtils.getOutputSchema(
                sampleOutput, new PropertyFactory(sampleOutput)));

        return save(workflowId, workflowNodeName, workflowNodeType, outputSchema, sampleOutput);
    }

    @Override
    public WorkflowNodeTestOutput save(
        @NonNull String workflowId, @NonNull String workflowNodeName, @NonNull WorkflowNodeType workflowNodeType,
        @NonNull Output output) {

        return save(
            workflowId, workflowNodeName, workflowNodeType, output.getOutputSchema(), output.getSampleOutput());
    }

    @Override
    public void updateWorkflowId(String oldWorkflowId, String newWorkflowId) {
        workflowNodeTestOutputRepository.updateWorkflowId(oldWorkflowId, newWorkflowId);
    }

    private WorkflowNodeTestOutput save(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType,
        Property outputSchema, Object sampleOutput) {

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
