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
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_NAME;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_VERSION;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

public class DataStreamStreamActionDefinition extends AbstractActionDefinitionWrapper {

    private final Job job;
    private final JobLauncher jobLauncher;

    public DataStreamStreamActionDefinition(ActionDefinition actionDefinition, Job job, JobLauncher jobLauncher) {
        super(actionDefinition);

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

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        JobParameters jobParameters = new JobParameters(
            new HashMap<>() {
                {
                    ClusterElement clusterElement = clusterElementMap.getClusterElement(DESTINATION);

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

                    put(DESTINATION.name(), new JobParameter<>(value, Map.class));

                    if (actionContextAware.getJobPrincipalId() != null) {
                        put(PRINCIPAL_ID, new JobParameter<>(actionContextAware.getJobPrincipalId(), Long.class));
                    }

                    if (actionContextAware.getJobPrincipalWorkflowId() != null) {
                        put(
                            PRINCIPAL_WORKFLOW_ID,
                            new JobParameter<>(actionContextAware.getJobPrincipalWorkflowId(), Long.class));
                    }

                    put(JOB_ID, new JobParameter<>(actionContextAware.getJobId(), Long.class));

                    clusterElement = clusterElementMap.getClusterElement(SOURCE);

                    value = new HashMap<>();

                    componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

                    if (componentConnection != null) {
                        value.put(COMPONENT_CONNECTION, componentConnection);
                    }

                    value.put(COMPONENT_NAME, clusterElement.getComponentName());
                    value.put(COMPONENT_VERSION, clusterElement.getComponentVersion());
                    value.put(CLUSTER_ELEMENT_NAME, clusterElement.getClusterElementName());

                    value.put(INPUT_PARAMETERS, clusterElement.getParameters());

                    put(SOURCE.name(), new JobParameter<>(value, Map.class));

                    put(TENANT_ID, new JobParameter<>(TenantContext.getCurrentTenantId(), String.class));
                    put(
                        MetadataConstants.EDITOR_ENVIRONMENT,
                        new JobParameter<>(actionContextAware.isEditorEnvironment(), Boolean.class));

                    if (actionContextAware.getPlatformType() != null) {
                        put(
                            MODE_TYPE,
                            new JobParameter<>(String.valueOf(actionContextAware.getPlatformType()), String.class));
                    }
                }
            });

        JobExecution jobExecution = TenantContext.callWithTenantId(
            TenantContext.DEFAULT_TENANT_ID, () -> jobLauncher.run(job, jobParameters));

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
}
