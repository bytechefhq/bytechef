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

package com.bytechef.automation.knowledgebase.trigger;

import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
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
 * Phase 17b: turns a workflow's {@code metadata.knowledgeBaseSourceId} into a
 * {@code datastream.since = lastSyncRunAt.toEpochMilli()} Spring Batch {@code JobParameter} entry. The reader picks
 * this up via Phase 17's {@code ItemStreamReaderDelegate} (which copies into per-step
 * {@code ExecutionContext.SINCE_KEY}) and filters upstream — Airtable today, more readers as they opt into the SPI via
 * {@code supportsIncremental() = true}.
 *
 * <p>
 * Returns {@code Map.of()} for any "no signal" outcome: metadata is missing the discriminator key, the source row was
 * deleted between trigger fire and job creation, or the source has never run before ({@code lastSyncRunAt == null}).
 * The reader treats a missing {@code SINCE_KEY} as "first run = full pull", which is the right behavior in every
 * fallback case.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseSourceTriggerJobParameterContributor implements TriggerJobParameterContributor {

    /**
     * Metadata key the workflow generator stores the source id under. See
     * {@code KnowledgeBaseSourceWorkflowGenerator.METADATA_KNOWLEDGE_BASE_SOURCE_ID}.
     */
    private static final String METADATA_KEY = "knowledgeBaseSourceId";

    private static final Logger log =
        LoggerFactory.getLogger(KnowledgeBaseSourceTriggerJobParameterContributor.class);

    private final KnowledgeBaseSourceService knowledgeBaseSourceService;

    @SuppressFBWarnings("EI2")
    public KnowledgeBaseSourceTriggerJobParameterContributor(KnowledgeBaseSourceService knowledgeBaseSourceService) {
        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
    }

    @Override
    public Map<String, ?> contribute(Map<String, ?> workflowMetadataMap, WorkflowExecutionId workflowExecutionId) {
        Object sourceIdValue = workflowMetadataMap.get(METADATA_KEY);

        if (sourceIdValue == null) {
            return Map.of();
        }

        long sourceId = toLong(sourceIdValue);

        Optional<KnowledgeBaseSource> sourceOptional;

        try {
            sourceOptional = knowledgeBaseSourceService.fetch(sourceId);
        } catch (RuntimeException ex) {
            // Lookup failures (DB down, source removed mid-flight) collapse to "first run" semantics — the reader
            // does a full pull and the next sync recovers. Logging at WARN so an oncall can spot a sustained
            // pattern without pager noise.
            log.warn("Failed to fetch KnowledgeBaseSource {} for trigger job parameter contribution: {}",
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
