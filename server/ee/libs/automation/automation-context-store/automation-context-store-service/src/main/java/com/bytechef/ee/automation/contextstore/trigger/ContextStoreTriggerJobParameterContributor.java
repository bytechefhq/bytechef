/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.trigger;

import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.TriggerJobParameterContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Phase 17b: turns a workflow's {@code metadata.contextStoreSourceId} into a
 * {@code datastream.since = lastSyncRunAt.toEpochMilli()} Spring Batch {@code JobParameter} entry. Mirrors the KB-side
 * {@code KnowledgeBaseSourceTriggerJobParameterContributor}; see that class's Javadoc for the contract details. The two
 * contributors key off different metadata fields so they coexist in the coordinator's contributor list without
 * colliding.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreTriggerJobParameterContributor implements TriggerJobParameterContributor {

    /**
     * Metadata key the workflow generator stores the source id under. See
     * {@code ContextStoreWorkflowGenerator.METADATA_CONTEXT_STORE_SOURCE_ID}.
     */
    private static final String METADATA_KEY = "contextStoreSourceId";

    private static final Logger log = LoggerFactory.getLogger(ContextStoreTriggerJobParameterContributor.class);

    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI2")
    public ContextStoreTriggerJobParameterContributor(ContextStoreSourceService contextStoreSourceService) {
        this.contextStoreSourceService = contextStoreSourceService;
    }

    @Override
    public Map<String, ?> contribute(Map<String, ?> workflowMetadataMap, WorkflowExecutionId workflowExecutionId) {
        Object sourceIdValue = workflowMetadataMap.get(METADATA_KEY);

        if (sourceIdValue == null) {
            return Map.of();
        }

        long sourceId = toLong(sourceIdValue);

        Optional<ContextStoreSource> sourceOptional;

        try {
            sourceOptional = contextStoreSourceService.fetch(sourceId);
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch ContextStoreSource {} for trigger job parameter contribution: {}",
                sourceId, ex.getMessage());

            return Map.of();
        }

        if (sourceOptional.isEmpty() || sourceOptional.get()
            .getLastSyncRunAt() == null) {
            return Map.of();
        }

        return Map.of(ItemReader.SINCE_KEY, sourceOptional.get()
            .getLastSyncRunAt()
            .toEpochMilli());
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }
}
