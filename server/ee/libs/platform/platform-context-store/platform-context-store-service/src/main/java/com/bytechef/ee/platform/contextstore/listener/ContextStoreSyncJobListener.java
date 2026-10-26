/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.listener;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordIndexService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreSyncJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreSyncJobListener.class);

    private final ContextStoreRecordIndexService contextStoreRecordIndexService;
    private final ContextStoreRecordService contextStoreRecordService;
    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI2")
    public ContextStoreSyncJobListener(
        ContextStoreRecordIndexService contextStoreRecordIndexService,
        ContextStoreRecordService contextStoreRecordService, ContextStoreSourceService contextStoreSourceService) {

        this.contextStoreRecordIndexService = contextStoreRecordIndexService;
        this.contextStoreRecordService = contextStoreRecordService;
        this.contextStoreSourceService = contextStoreSourceService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        ContextStoreSyncJobParameters parameters = ContextStoreSyncJobParameters.from(jobExecution);

        if (parameters == null) {
            return;
        }

        Long sourceId = parameters.sourceId();
        String mode = parameters.mode();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            Instant now = Instant.now();

            if (ContextStoreSyncJobParameters.MODE_PARTIAL.equals(mode)) {
                log.info("Context Store sync completed in PARTIAL mode: source={}", sourceId);

                contextStoreSourceService.updateLastSyncMetadata(sourceId, now, jobExecution.getId());
            } else {
                Set<String> seenIds = aggregateSeenIds(jobExecution);

                int tombstoned = contextStoreRecordService.tombstoneUnseen(sourceId, seenIds, now);

                int purgedIndexRows = purgeTombstonedIndexRows(sourceId);

                log.info(
                    "Context Store sync completed in FULL_REPLACE mode: source={} seen={} tombstoned={} " +
                        "purgedIndexRows={}",
                    sourceId, seenIds.size(), tombstoned, purgedIndexRows);

                contextStoreSourceService.updateStatus(
                    sourceId, ContextStoreSourceStatus.READY, now, jobExecution.getId());
            }
        } else {
            log.warn(
                "Context Store sync failed: source={} status={} errors={}",
                sourceId, jobExecution.getStatus(), jobExecution.getAllFailureExceptions());

            Optional<ContextStoreSource> sourceOptional = contextStoreSourceService.fetch(sourceId);

            if (sourceOptional.isEmpty()) {
                return;
            }

            ContextStoreSourceStatus current = sourceOptional.get()
                .getStatus();

            if (current != ContextStoreSourceStatus.READY) {
                contextStoreSourceService.updateStatus(
                    sourceId, ContextStoreSourceStatus.FAILED, null, jobExecution.getId());
            }
        }
    }

    private int purgeTombstonedIndexRows(Long sourceId) {
        try {
            return contextStoreRecordIndexService.deleteAllForTombstonedRecords(sourceId);
        } catch (RuntimeException exception) {
            log.warn("Failed to purge index rows for tombstoned records: source={}", sourceId, exception);

            return 0;
        }
    }

    private static Set<String> aggregateSeenIds(JobExecution jobExecution) {
        Set<String> seenIds = new HashSet<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Object value = stepExecution.getExecutionContext()
                .get(ContextStoreExecutionContextKeys.SEEN_IDS_KEY);

            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String string) {
                        seenIds.add(string);
                    }
                }
            }
        }

        return seenIds;
    }
}
