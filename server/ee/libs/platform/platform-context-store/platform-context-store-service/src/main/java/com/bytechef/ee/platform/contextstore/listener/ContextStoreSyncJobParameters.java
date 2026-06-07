/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.listener;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;

/**
 * Parsed view of the {@code DESTINATION} job parameter on a Context Store sync job. Returned by
 * {@link #from(JobExecution)}; {@code null} when the job is not a {@code contextStore.writeToReplica} sync (different
 * component, missing destination, malformed parameters, etc.). Shared by the structured listener
 * ({@link ContextStoreSyncJobListener}) and the semantic listener ({@link ContextStoreSemanticBatchListener}).
 *
 * @author Ivica Cardic
 * @version ee
 */
record ContextStoreSyncJobParameters(Long sourceId, String mode) {

    static final String MODE_FULL_REPLACE = "FULL_REPLACE";
    static final String MODE_PARTIAL = "PARTIAL";

    private static final String DESTINATION_KEY = "DESTINATION";
    private static final String COMPONENT_NAME_KEY = "componentName";
    private static final String CLUSTER_ELEMENT_NAME_KEY = "clusterElementName";
    private static final String INPUT_PARAMETERS_KEY = "inputParameters";

    private static final String CONTEXT_STORE_COMPONENT_NAME = "contextStore";
    private static final String WRITE_TO_REPLICA_CLUSTER_ELEMENT_NAME = "writeToReplica";

    private static final String SOURCE_ID_PARAMETER = "sourceId";
    private static final String MODE_PARAMETER = "mode";

    static @Nullable ContextStoreSyncJobParameters from(JobExecution jobExecution) {
        JobParameter<?> destinationParameter = jobExecution.getJobParameters()
            .getParameter(DESTINATION_KEY);

        if (destinationParameter == null || !(destinationParameter.value() instanceof Map<?, ?> destination)) {
            return null;
        }

        if (!CONTEXT_STORE_COMPONENT_NAME.equals(destination.get(COMPONENT_NAME_KEY))
            || !WRITE_TO_REPLICA_CLUSTER_ELEMENT_NAME.equals(destination.get(CLUSTER_ELEMENT_NAME_KEY))) {

            return null;
        }

        if (!(destination.get(INPUT_PARAMETERS_KEY) instanceof Map<?, ?> inputParameters)) {
            return null;
        }

        Long sourceId = coerceLong(inputParameters.get(SOURCE_ID_PARAMETER));

        if (sourceId == null) {
            return null;
        }

        Object modeValue = inputParameters.get(MODE_PARAMETER);

        String mode = modeValue instanceof String string && !string.isEmpty()
            ? string
            : MODE_FULL_REPLACE;

        return new ContextStoreSyncJobParameters(sourceId, mode);
    }

    private static @Nullable Long coerceLong(@Nullable Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isEmpty()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
