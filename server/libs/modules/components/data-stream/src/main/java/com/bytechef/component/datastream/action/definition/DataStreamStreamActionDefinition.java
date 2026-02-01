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

package com.bytechef.component.datastream.action.definition;

import static com.bytechef.component.datastream.constant.DataStreamConstants.CLUSTER_ELEMENT_NAME;
import static com.bytechef.component.datastream.constant.DataStreamConstants.COMPONENT_CONNECTION;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INPUT_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.JOB_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.MODE_TYPE;
import static com.bytechef.component.datastream.constant.DataStreamConstants.PRINCIPAL_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.PRINCIPAL_WORKFLOW_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TENANT_ID;
import static com.bytechef.component.definition.datastream.ItemReader.SOURCE;
import static com.bytechef.component.definition.datastream.ItemWriter.DESTINATION;
import static com.bytechef.platform.component.definition.datastream.ItemProcessor.PROCESSOR;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_NAME;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_VERSION;

import com.bytechef.component.datastream.batch.InMemoryBatchJobFactory;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

public class DataStreamStreamActionDefinition extends AbstractActionDefinitionWrapper {

    private final InMemoryBatchJobFactory inMemoryBatchJobFactory;
    private final Job job;
    private final JobLauncher jobLauncher;

    public DataStreamStreamActionDefinition(
        ActionDefinition actionDefinition, Job job, JobLauncher jobLauncher,
        InMemoryBatchJobFactory inMemoryBatchJobFactory) {

        super(actionDefinition);

        this.inMemoryBatchJobFactory = inMemoryBatchJobFactory;
        this.job = job;
        this.jobLauncher = jobLauncher;
    }

    @Override
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((MultipleConnectionsPerformFunction) this::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ? extends ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext actionContext) throws Exception {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        boolean editorEnvironment = actionContextAware.isEditorEnvironment();

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        JobParameters jobParameters = new JobParameters(
            new HashSet<>() {
                {
                    ClusterElement clusterElement = clusterElementMap.getClusterElement(DESTINATION);

                    Map<String, Object> value = getValue(clusterElement, connectionParameters);

                    add(new JobParameter<>(DESTINATION.name(), value, Map.class));

                    if (actionContextAware.getJobPrincipalId() != null) {
                        add(new JobParameter<>(PRINCIPAL_ID, actionContextAware.getJobPrincipalId(), Long.class));
                    }

                    if (actionContextAware.getJobPrincipalWorkflowId() != null) {
                        add(
                            new JobParameter<>(
                                PRINCIPAL_WORKFLOW_ID, actionContextAware.getJobPrincipalWorkflowId(), Long.class));
                    }

                    if (actionContextAware.getJobId() != null) {
                        add(new JobParameter<>(JOB_ID, actionContextAware.getJobId(), Long.class));
                    }

                    clusterElementMap.fetchClusterElement(PROCESSOR)
                        .ifPresent(processorClusterElement -> {
                            Map<String, Object> processorValue =
                                getValue(processorClusterElement, connectionParameters);

                            add(new JobParameter<>(PROCESSOR.name(), processorValue, Map.class));
                        });

                    clusterElement = clusterElementMap.getClusterElement(SOURCE);

                    value = getValue(clusterElement, connectionParameters);

                    add(new JobParameter<>(SOURCE.name(), value, Map.class));

                    add(new JobParameter<>(TENANT_ID, TenantContext.getCurrentTenantId(), String.class));
                    add(
                        new JobParameter<>(
                            MetadataConstants.EDITOR_ENVIRONMENT, editorEnvironment, Boolean.class));

                    if (actionContextAware.getPlatformType() != null) {
                        add(
                            new JobParameter<>(
                                MODE_TYPE, String.valueOf(actionContextAware.getPlatformType()), String.class));
                    }
                }
            });

        JobExecution jobExecution;

        if (editorEnvironment) {
            jobExecution = TenantContext.callWithTenantId(
                TenantContext.DEFAULT_TENANT_ID, () -> inMemoryBatchJobFactory.runJob(jobParameters));
        } else {
            jobExecution = TenantContext.callWithTenantId(
                TenantContext.DEFAULT_TENANT_ID, () -> jobLauncher.run(job, jobParameters));
        }

        List<Throwable> failureExceptions = jobExecution.getAllFailureExceptions();

        if (!failureExceptions.isEmpty()) {
            throw new ProviderException(
                failureExceptions.stream()
                    .map(Throwable::getMessage)
                    .collect(Collectors.joining(",")));
        }

        return Map.of(
            "endTime", Objects.requireNonNull(jobExecution.getEndTime()),
            "status", jobExecution.getStatus(),
            "startTime", Objects.requireNonNull(jobExecution.getStartTime()));
    }

    private static @NonNull Map<String, Object> getValue(
        ClusterElement clusterElement, Map<String, ? extends ComponentConnection> connectionParameters) {

        Map<String, Object> value = new HashMap<>();

        ComponentConnection componentConnection = connectionParameters.get(
            clusterElement.getWorkflowNodeName());

        if (componentConnection != null) {
            value.put(COMPONENT_CONNECTION, componentConnection);
        }

        value.put(COMPONENT_NAME, clusterElement.getComponentName());
        value.put(COMPONENT_VERSION, clusterElement.getComponentVersion());
        value.put(CLUSTER_ELEMENT_NAME, clusterElement.getClusterElementName());

        value.put(INPUT_PARAMETERS, clusterElement.getParameters());
        return value;
    }
}
