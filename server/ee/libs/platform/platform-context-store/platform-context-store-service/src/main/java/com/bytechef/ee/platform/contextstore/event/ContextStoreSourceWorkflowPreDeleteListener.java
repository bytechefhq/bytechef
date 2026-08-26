/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.event;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Clears the sync-workflow pointer of any context store source whose generated workflow is deleted.
 *
 * <p>
 * The same shape and the same reasoning as {@code KnowledgeBaseSourceWorkflowPreDeleteListener} (referenced by name
 * rather than linked: that class is CE and this module does not depend on it): {@code context_store_source.workflow_id}
 * is a nullable column with no foreign key, and {@code null} is an already-supported state that the facade guards on
 * everywhere. The pointer is cleared rather than the source deleted, because a source owns ingested content and
 * configuration that a workflow delete was never asked to remove.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreSourceWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreSourceWorkflowPreDeleteListener.class);

    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI")
    public ContextStoreSourceWorkflowPreDeleteListener(ContextStoreSourceService contextStoreSourceService) {
        this.contextStoreSourceService = contextStoreSourceService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        List<ContextStoreSource> contextStoreSources = contextStoreSourceService.findAllByWorkflowId(workflowId);

        if (contextStoreSources.isEmpty()) {
            return;
        }

        log.debug(
            "Clearing the workflow pointer of {} context store source(s) for workflow {}", contextStoreSources.size(),
            workflowId);

        for (ContextStoreSource contextStoreSource : contextStoreSources) {
            contextStoreSource.setWorkflowId(null);

            contextStoreSourceService.update(contextStoreSource);
        }
    }
}
