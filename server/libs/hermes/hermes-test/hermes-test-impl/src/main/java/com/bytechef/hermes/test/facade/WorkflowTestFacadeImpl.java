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

package com.bytechef.hermes.test.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.domain.WorkflowTrigger;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import com.bytechef.hermes.execution.dto.TriggerExecutionDTO;
import com.bytechef.hermes.test.domain.WorkflowTestComponentDefinition;
import com.bytechef.hermes.test.domain.WorkflowTestConfiguration;
import com.bytechef.hermes.test.domain.WorkflowTestConfigurationConnection;
import com.bytechef.hermes.test.dto.WorkflowTestExecution;
import com.bytechef.hermes.test.executor.JobTestExecutor;
import com.bytechef.hermes.test.service.WorkflowTestComponentDefinitionService;
import com.bytechef.hermes.test.service.WorkflowTestConfigurationService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowTestFacadeImpl implements WorkflowTestFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final JobTestExecutor jobTestExecutor;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowService workflowService;
    private final WorkflowTestComponentDefinitionService workflowTestComponentDefinitionService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowTestFacadeImpl(
        ComponentDefinitionService componentDefinitionService, JobTestExecutor jobTestExecutor,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService,
        WorkflowTestComponentDefinitionService workflowTestComponentDefinitionService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentDefinitionService = componentDefinitionService;
        this.jobTestExecutor = jobTestExecutor;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
        this.workflowTestComponentDefinitionService = workflowTestComponentDefinitionService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @SuppressWarnings("unchecked")
    public WorkflowTestExecution testWorkflow(String workflowId) {
        Optional<WorkflowTestConfiguration> workflowTestConfigurationOptional =
            workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId);

        Map<String, ?> inputs = OptionalUtils.mapOrElse(
            workflowTestConfigurationOptional, WorkflowTestConfiguration::getInputs, Map.of());

        Map<String, Map<String, Long>> connectionIdsMap = new HashMap<>();

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections = OptionalUtils.mapOrElse(
            workflowTestConfigurationOptional, WorkflowTestConfiguration::getConnections, List.of());

        for (WorkflowTestConfigurationConnection connection : workflowTestConfigurationConnections) {
            Map<String, Long> connectionIdMap = connectionIdsMap.computeIfAbsent(
                connection.getOperationName(), key -> new HashMap<>());

            connectionIdMap.put(connection.getKey(), connection.getId());
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        TriggerExecutionDTO triggerExecutionDTO = null;

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        if (!workflowTriggers.isEmpty()) {
            WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

            OperationType operationType = OperationType.ofType(workflowTrigger.getType());

            Object sampleOutput = OptionalUtils.mapOrElseGet(
                workflowTestComponentDefinitionService.fetchWorkflowTestComponentDefinition(
                    workflowId, workflowTrigger.getName()),
                WorkflowTestComponentDefinition::getSampleOutput,
                () -> {
                    TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                        operationType.componentName(), operationType.componentVersion(),
                        operationType.componentOperationName());

                    return triggerDefinition.getSampleOutput();
                });

            if (sampleOutput == null) {
                throw new IllegalStateException("\"sampleOutput\" value is not defined");
            }

            TriggerExecution triggerExecution = TriggerExecution.builder()
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now())
                .status(Status.COMPLETED)
                .workflowTrigger(workflowTrigger)
                .build();

            triggerExecutionDTO = new TriggerExecutionDTO(
                componentDefinitionService.getComponentDefinition(
                    operationType.componentName(), operationType.componentVersion()),
                inputs, sampleOutput,
                triggerExecution);

            inputs = MapUtils.concat((Map<String, Object>) inputs, Map.of(workflowTrigger.getName(), sampleOutput));
        }

        return new WorkflowTestExecution(
            jobTestExecutor.execute(
                new JobParameters(workflowId, inputs, Map.of(MetadataConstants.CONNECTION_IDS, connectionIdsMap))),
            triggerExecutionDTO);
    }
}
