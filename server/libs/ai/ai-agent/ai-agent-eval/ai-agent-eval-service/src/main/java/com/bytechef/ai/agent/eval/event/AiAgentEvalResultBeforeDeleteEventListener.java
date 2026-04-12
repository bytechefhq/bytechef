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

package com.bytechef.ai.agent.eval.event;

import com.bytechef.ai.agent.eval.domain.AiAgentEvalResult;
import com.bytechef.ai.agent.eval.file.storage.AiAgentEvalFileStorage;
import com.bytechef.ai.agent.eval.service.AiAgentEvalResultService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * Cleans up transcript files from file storage before an {@link AiAgentEvalResult} is deleted.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentEvalResultBeforeDeleteEventListener extends AbstractRelationalEventListener<AiAgentEvalResult> {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentEvalResultBeforeDeleteEventListener.class);

    private final AiAgentEvalFileStorage agentEvalFileStorage;
    private final AiAgentEvalResultService agentEvalResultService;

    @SuppressFBWarnings("EI")
    public AiAgentEvalResultBeforeDeleteEventListener(
        AiAgentEvalFileStorage agentEvalFileStorage, AiAgentEvalResultService agentEvalResultService) {

        this.agentEvalFileStorage = agentEvalFileStorage;
        this.agentEvalResultService = agentEvalResultService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<AiAgentEvalResult> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        long resultId = (Long) identifier.getValue();

        AiAgentEvalResult result = agentEvalResultService.getAgentEvalResult(resultId);

        FileEntry transcriptFileEntry = result.getTranscriptFileEntry();

        if (transcriptFileEntry != null) {
            try {
                agentEvalFileStorage.deleteTranscriptFile(transcriptFileEntry);
            } catch (Exception exception) {
                logger.warn("Failed to delete transcript file for result {}", resultId, exception);
            }
        }
    }
}
