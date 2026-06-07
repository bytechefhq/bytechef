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

        // Phase 17b: extract trigger-time JobParameter overrides from the parent Atlas Job's metadata BEFORE
        // building Spring Batch JobParameters. Two consumption paths:
        // 1) "datastream.mode" overrides the DESTINATION cluster element's baked "mode" parameter — folded into
        // the destination value Map below so the destination writer's existing inputParameters.getString(MODE)
        // sees the override transparently.
        // 2) Every other entry (e.g. datastream.since) is added as a top-level Spring Batch JobParameter the
        // reader-side ItemStreamReaderDelegate consumes via stepExecution.getJobParameters().
        //
        // The reserved key string "__jobParameters" matches JobMetadataKeys.JOB_PARAMETERS in
        // platform-workflow-coordinator-api. Inlined here to avoid pulling a coordinator-api dep into this lean
        // component module — see Phase 17b spec Layer 5 "Risks / open questions" for the dep-layering rationale.
        Map<String, ?> jobParameterOverrides = extractJobParameterOverrides(actionContextAware.getJobMetadata());

        JobParameters jobParameters = new JobParameters(
            new HashSet<>() {
                {
                    ClusterElement clusterElement = clusterElementMap.getClusterElement(DESTINATION);

                    Map<String, Object> value = getValue(clusterElement, connectionParameters);

                    // Override the destination's baked "mode" with the trigger-time override, if any. Per Phase 17b
                    // commit 4, the paired-cadence workflow generators emit jobParameters.datastream.mode = PARTIAL
                    // on the incremental trigger and FULL_REPLACE on the full trigger; this is where that takes
                    // effect at the writer's mode selector.
                    Object datastreamModeOverride = jobParameterOverrides.get("datastream.mode");

                    if (datastreamModeOverride != null) {
                        value.put("mode", String.valueOf(datastreamModeOverride));
                    }

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

                    // Phase 17b: add every other entry as a top-level Spring Batch JobParameter. datastream.mode
                    // was already consumed above as a destination-parameter override; the remaining entries
                    // (datastream.since today, more keys in future contributor extensions) flow through to the
                    // reader-side ItemStreamReaderDelegate.
                    for (Map.Entry<String, ?> entry : jobParameterOverrides.entrySet()) {
                        String overrideKey = entry.getKey();
                        Object overrideValue = entry.getValue();

                        if (overrideValue == null || "datastream.mode".equals(overrideKey)) {
                            continue;
                        }

                        // Spring Batch JobParameter is strongly typed — pick a class based on the value's
                        // runtime type. datastream.since arrives as Long (epoch millis from the contributor).
                        // Other types fall back to String via toString() so a future contributor adding a
                        // Boolean / Double override doesn't need this site updated, just the reader that
                        // consumes the parameter.
                        if (overrideValue instanceof Long longValue) {
                            add(new JobParameter<>(overrideKey, longValue, Long.class));
                        } else if (overrideValue instanceof Number numberValue) {
                            add(new JobParameter<>(overrideKey, numberValue.longValue(), Long.class));
                        } else if (overrideValue instanceof Boolean booleanValue) {
                            add(new JobParameter<>(overrideKey, booleanValue, Boolean.class));
                        } else {
                            add(new JobParameter<>(overrideKey, String.valueOf(overrideValue), String.class));
                        }
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

    /**
     * Phase 17b: pulls the trigger-time JobParameter override map out of the parent Atlas Job's metadata. The map sits
     * under the reserved key {@code "__jobParameters"} ({@code JobMetadataKeys.JOB_PARAMETERS} in
     * {@code platform-workflow-coordinator-api}). Returns an empty map for pre-17b workflows, editor-environment runs
     * without a persisted Job, or any malformed metadata shape — the action then proceeds with no overrides, exactly
     * the pre-17b behavior.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, ?> extractJobParameterOverrides(Map<String, Object> jobMetadata) {
        Object value = jobMetadata.get("__jobParameters");

        if (value instanceof Map<?, ?>) {
            return (Map<String, ?>) value;
        }

        return Map.of();
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
