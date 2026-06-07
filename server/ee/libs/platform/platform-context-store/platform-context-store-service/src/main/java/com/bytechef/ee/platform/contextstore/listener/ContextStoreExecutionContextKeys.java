/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.listener;

/**
 * Shared keys placed by the {@code contextStore.writeToReplica} item writer onto the per-step Spring Batch
 * {@code ExecutionContext}. The structured listener ({@link ContextStoreSyncJobListener}) consumes
 * {@link #SEEN_IDS_KEY}; the semantic listener ({@link ContextStoreSemanticBatchListener}) consumes
 * {@link #SEMANTIC_RECORD_IDS_KEY}.
 *
 * <p>
 * Constants are duplicated as {@code public static final} on the writer side
 * ({@code ContextStoreItemWriter.SEEN_IDS_KEY} and {@code SEMANTIC_RECORD_IDS_KEY}) to avoid forcing the EE component
 * module to depend on this service module.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class ContextStoreExecutionContextKeys {

    /**
     * Source-supplied record ids encountered during the run, used by the structured listener for the FULL_REPLACE
     * tombstone sweep.
     */
    static final String SEEN_IDS_KEY = "contextStore.seenIds";

    /**
     * Persisted {@code context_store_record.id} values for records upserted during the run, used by the semantic
     * listener to know which records to (re-)embed and to look up existing vector-store rows.
     */
    static final String SEMANTIC_RECORD_IDS_KEY = "contextStore.semanticRecordIds";

    private ContextStoreExecutionContextKeys() {
    }
}
