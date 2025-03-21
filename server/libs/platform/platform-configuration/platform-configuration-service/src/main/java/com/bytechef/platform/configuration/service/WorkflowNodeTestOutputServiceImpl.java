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
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
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
        workflowNodeTestOutputRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName)
            .ifPresent(workflowNodeTestOutputRepository::delete);
    }

    @Override
    public boolean checkWorkflowNodeTestOutputExists(
        String workflowId, String workflowNodeName, @Nullable Instant createdDate) {

        if (createdDate == null) {
            return workflowNodeTestOutputRepository.existsByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName);
        } else {
            return workflowNodeTestOutputRepository.existsByWorkflowIdAndWorkflowNodeNameAndLastModifiedDateAfter(
                workflowId, workflowNodeName, createdDate);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowNodeTestOutput> fetchWorkflowTestNodeOutput(
        String workflowId, String workflowNodeName) {

        return workflowNodeTestOutputRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName);
    }

    @Override
    public void removeUnusedNodeTestOutputs(Workflow workflow) {
        List<String> workflowTaskNames = workflow.getTasks(true)
            .stream()
            .map(WorkflowTask::getName)
            .toList();

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow)
            .stream()
            .toList();

        List<String> workflowTriggerNames = WorkflowTrigger.of(workflow)
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

            if (workflowTriggerNames.contains(workflowNodeTestOutput.getWorkflowNodeName())) {
                workflowTriggers.stream()
                    .filter(workflowTrigger -> {
                        String name = workflowTrigger.getName();

                        return name.equals(workflowNodeTestOutput.getWorkflowNodeName());
                    })
                    .findFirst()
                    .ifPresent(workflowTrigger -> {
                        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                        if (!Objects.equals(
                            workflowNodeType.operation(),
                            workflowNodeTestOutput.getComponentOperationName())) {

                            workflowNodeTestOutputRepository.delete(workflowNodeTestOutput);
                        }
                    });
            }
        }
    }

    @Override
    public WorkflowNodeTestOutput save(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType, OutputResponse outputResponse) {

        return save(
            workflowId, workflowNodeName, workflowNodeType, (Property) outputResponse.outputSchema(),
            outputResponse.sampleOutput());
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

        workflowNodeTestOutput.setComponentName(workflowNodeType.name());
        workflowNodeTestOutput.setComponentOperationName(workflowNodeType.operation());
        workflowNodeTestOutput.setComponentVersion(workflowNodeType.version());
        workflowNodeTestOutput.setOutputSchema(outputSchema);
        workflowNodeTestOutput.setSampleOutput(sampleOutput);
        workflowNodeTestOutput.setWorkflowId(workflowId);
        workflowNodeTestOutput.setWorkflowNodeName(workflowNodeName);

        return workflowNodeTestOutputRepository.save(workflowNodeTestOutput);
    }
}
