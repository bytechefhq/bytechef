/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.workflow.test.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution.Status;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import com.bytechef.platform.workflow.test.executor.JobTestExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowTestFacadeImpl implements WorkflowTestFacade {

    private static final Random RANDOM = new Random();

    private final ComponentDefinitionService componentDefinitionService;
    private final JobTestExecutor jobTestExecutor;
    private final WorkflowService workflowService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowTestFacadeImpl(
        ComponentDefinitionService componentDefinitionService, JobTestExecutor jobTestExecutor,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentDefinitionService = componentDefinitionService;
        this.jobTestExecutor = jobTestExecutor;
        this.workflowService = workflowService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @SuppressWarnings("unchecked")
    public WorkflowTestExecutionDTO testWorkflow(String workflowId, Map<String, Object> inputs, long environmentId) {
        Optional<WorkflowTestConfiguration> workflowTestConfigurationOptional =
            workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId);

        Map<String, Map<String, Long>> connectionIdsMap = new HashMap<>();

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationOptional
                .map(WorkflowTestConfiguration::getConnections)
                .orElse(List.of());

        for (WorkflowTestConfigurationConnection connection : workflowTestConfigurationConnections) {
            Map<String, Long> connectionIdMap = connectionIdsMap.computeIfAbsent(
                connection.getWorkflowNodeName(), key -> new HashMap<>());

            connectionIdMap.put(connection.getWorkflowConnectionKey(), connection.getConnectionId());
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        TriggerExecutionDTO triggerExecutionDTO = null;

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        if (workflowTriggers.isEmpty()) {
            Map<String, ?> workflowTestConfigurationInputs = OptionalUtils.mapOrElse(
                workflowTestConfigurationOptional, WorkflowTestConfiguration::getInputs, Map.of());

            inputs = MapUtils.concat(inputs, (Map<String, Object>) workflowTestConfigurationInputs);
        } else {
            WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
                workflowId, workflowTrigger.getName(), environmentId);

            if (inputs.isEmpty()) {
                TriggerExecution triggerExecution = TriggerExecution.builder()
                    .id(-RANDOM.nextLong())
                    .startDate(Instant.now())
                    .endDate(Instant.now())
                    .status(Status.COMPLETED)
                    .workflowTrigger(workflowTrigger)
                    .build();

                ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                    workflowNodeType.name(), workflowNodeType.version());

                Map<String, ?> workflowTestConfigurationInputs = OptionalUtils.mapOrElse(
                    workflowTestConfigurationOptional, WorkflowTestConfiguration::getInputs, Map.of());

                Object sampleOutput = workflowNodeOutputDTO.getSampleOutput();

                if (sampleOutput == null) {
                    sampleOutput = Map.of();
                }

                triggerExecutionDTO = new TriggerExecutionDTO(
                    triggerExecution, componentDefinition.getTitle(), componentDefinition.getIcon(),
                    workflowTestConfigurationInputs, sampleOutput);

                WorkflowNodeType triggerNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
                    .stream()
                    .filter(curTriggerDefinition -> Objects.equals(
                        curTriggerDefinition.getName(), triggerNodeType.operation()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Trigger definition not found"));

                if (!triggerDefinition.isBatch() && sampleOutput instanceof List<?> list) {
                    sampleOutput = list.getFirst();
                }

                inputs = MapUtils.concat(
                    (Map<String, Object>) workflowTestConfigurationInputs,
                    Map.of(workflowTrigger.getName(), sampleOutput));
            }
        }

        return new WorkflowTestExecutionDTO(
            jobTestExecutor.execute(
                new JobParametersDTO(workflowId, inputs, Map.of(MetadataConstants.CONNECTION_IDS, connectionIdsMap))),
            triggerExecutionDTO);
    }
}
