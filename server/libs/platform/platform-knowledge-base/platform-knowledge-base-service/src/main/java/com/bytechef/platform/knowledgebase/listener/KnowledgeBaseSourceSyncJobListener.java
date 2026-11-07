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

package com.bytechef.platform.knowledgebase.listener;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseSourceSyncJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSourceSyncJobListener.class);

    private static final String DESTINATION_KEY = "DESTINATION";
    private static final String COMPONENT_NAME_KEY = "componentName";
    private static final String CLUSTER_ELEMENT_NAME_KEY = "clusterElementName";
    private static final String INPUT_PARAMETERS_KEY = "inputParameters";

    private static final String KNOWLEDGE_BASE_COMPONENT_NAME = "knowledgeBase";
    private static final String WRITE_AS_DOCUMENT_CLUSTER_ELEMENT_NAME = "writeAsDocument";

    private static final String SOURCE_ID_PARAMETER = "sourceId";
    private static final String MODE_PARAMETER = "mode";

    private static final String MODE_FULL_REPLACE = "FULL_REPLACE";
    private static final String MODE_PARTIAL = "PARTIAL";

    private static final String SEEN_RECORD_IDS_KEY = "knowledgeBaseSource.seenRecordIds";

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseSourceService knowledgeBaseSourceService;

    @SuppressFBWarnings("EI2")
    public KnowledgeBaseSourceSyncJobListener(
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseSourceService knowledgeBaseSourceService) {

        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        KnowledgeBaseSyncJobParameters parameters = parseJobParameters(jobExecution);

        if (parameters == null) {
            return;
        }

        Long sourceId = parameters.sourceId();
        String mode = parameters.mode();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            Instant now = Instant.now();

            if (MODE_PARTIAL.equals(mode)) {
                log.info("Knowledge Base sync completed in PARTIAL mode: source={}", sourceId);

                knowledgeBaseSourceService.updateLastSyncMetadata(sourceId, now, jobExecution.getId());
            } else {
                Set<String> seenRecordIds = aggregateSeenRecordIds(jobExecution);

                int tombstoned = knowledgeBaseDocumentService.tombstoneUnseen(sourceId, seenRecordIds, now);

                int sweptChunks = sweepTombstonedDocumentChunks(sourceId);

                log.info(
                    "Knowledge Base sync completed in FULL_REPLACE mode: source={} seen={} tombstoned={} " +
                        "sweptChunks={}",
                    sourceId, seenRecordIds.size(), tombstoned, sweptChunks);

                knowledgeBaseSourceService.updateStatus(
                    sourceId, KnowledgeBaseSourceStatus.READY, now, jobExecution.getId());
            }
        } else {
            log.warn(
                "Knowledge Base sync failed: source={} status={} errors={}",
                sourceId, jobExecution.getStatus(), jobExecution.getAllFailureExceptions());

            Optional<KnowledgeBaseSource> sourceOptional = knowledgeBaseSourceService.fetch(sourceId);

            if (sourceOptional.isEmpty()) {
                return;
            }

            KnowledgeBaseSourceStatus current = sourceOptional.get()
                .getStatus();

            if (current != KnowledgeBaseSourceStatus.READY) {
                knowledgeBaseSourceService.updateStatus(
                    sourceId, KnowledgeBaseSourceStatus.FAILED, null, jobExecution.getId());
            } else {
                knowledgeBaseSourceService.updateLastSyncMetadata(sourceId, null, jobExecution.getId());
            }
        }
    }

    private int sweepTombstonedDocumentChunks(Long sourceId) {
        try {
            return knowledgeBaseDocumentFacade.sweepTombstonedDocumentChunks(sourceId);
        } catch (RuntimeException exception) {
            log.warn(
                "Knowledge Base tombstone chunk sweep failed: source={} — structured sync remains intact",
                sourceId, exception);

            return 0;
        }
    }

    private static @Nullable KnowledgeBaseSyncJobParameters parseJobParameters(JobExecution jobExecution) {
        JobParameter<?> destinationParameter = jobExecution.getJobParameters()
            .getParameter(DESTINATION_KEY);

        if (destinationParameter == null || !(destinationParameter.value() instanceof Map<?, ?> destination)) {
            return null;
        }

        if (!KNOWLEDGE_BASE_COMPONENT_NAME.equals(destination.get(COMPONENT_NAME_KEY))
            || !WRITE_AS_DOCUMENT_CLUSTER_ELEMENT_NAME.equals(destination.get(CLUSTER_ELEMENT_NAME_KEY))) {

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

        return new KnowledgeBaseSyncJobParameters(sourceId, mode);
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

    private static Set<String> aggregateSeenRecordIds(JobExecution jobExecution) {
        Set<String> seenRecordIds = new HashSet<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Object value = stepExecution.getExecutionContext()
                .get(SEEN_RECORD_IDS_KEY);

            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String string) {
                        seenRecordIds.add(string);
                    }
                }
            }
        }

        return seenRecordIds;
    }

    private record KnowledgeBaseSyncJobParameters(Long sourceId, String mode) {
    }
}
