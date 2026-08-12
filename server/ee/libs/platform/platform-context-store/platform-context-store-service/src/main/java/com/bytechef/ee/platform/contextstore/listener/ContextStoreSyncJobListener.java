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
 * {@link JobExecutionListener} that detects DataStream sync jobs whose DESTINATION cluster element is
 * {@code contextStore.writeToReplica} and applies mode-aware status updates plus an optional tombstone sweep.
 *
 * <p>
 * On COMPLETED: the listener reads the {@code mode} parameter from the destination's {@code inputParameters} (defaults
 * to {@code FULL_REPLACE} when missing — backward compatible with workflows that pre-date the parameter):
 * <ul>
 * <li>{@code FULL_REPLACE}: aggregates {@code seenIds} from each step's {@code ExecutionContext} (populated by
 * {@code ContextStoreItemWriter.update()} under the key {@code "contextStore.seenIds"}), calls
 * {@link ContextStoreRecordService#tombstoneUnseen}, purges the tombstoned records' sidecar index rows via
 * {@link ContextStoreRecordIndexService#deleteAllForTombstonedRecords} (best-effort — a purge failure never blocks the
 * status flip), then flips status to {@code READY} along with {@code lastSyncRunAt} and
 * {@code lastSyncJobExecutionId}.</li>
 * <li>{@code PARTIAL}: skips the tombstone sweep entirely and updates {@code lastSyncRunAt} +
 * {@code lastSyncJobExecutionId} without touching status — a {@code BUILDING_PREVIEW} source stays
 * {@code BUILDING_PREVIEW} until a {@code FULL_REPLACE} run completes; a {@code READY} source stays {@code READY}.</li>
 * </ul>
 *
 * <p>
 * On FAILED: updates the source status to {@code FAILED} unless the source was previously {@code READY} — preserving
 * the last good state so a transient sync failure doesn't downgrade a working source.
 *
 * <p>
 * Non-Context-Store DataStream jobs are passed through unchanged: the listener inspects the {@code DESTINATION} job
 * parameter and short-circuits when the component/cluster-element name doesn't match.
 *
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

    /**
     * Best-effort purge of the sidecar index rows belonging to tombstoned records — runs on every FULL_REPLACE sweep
     * (not only when this sweep tombstoned something) so rows left behind by earlier runs self-heal. A purge failure
     * must not block the READY status flip, mirroring the Knowledge Base listener's chunk sweep.
     */
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
