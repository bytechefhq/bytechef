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

package com.bytechef.component.ai.agent.chat.memory.session.compaction;

import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionRequest;
import org.springframework.ai.session.compaction.CompactionTrigger;

/**
 * Fires once a session holds more root, non-synthetic events than {@code maxEvents} — the same events the sliding
 * window and recursive summarization strategies count. A turn-based trigger under-counts them: one tool-using turn
 * stores several events.
 *
 * @author Ivica Cardic
 */
public final class EventCountTrigger implements CompactionTrigger {

    private final int maxEvents;

    public EventCountTrigger(int maxEvents) {
        if (maxEvents <= 0) {
            throw new IllegalArgumentException("maxEvents must be greater than 0");
        }

        this.maxEvents = maxEvents;
    }

    @Override
    public boolean shouldCompact(CompactionRequest compactionRequest) {
        long rootEventCount = compactionRequest.events()
            .stream()
            .filter(event -> !event.isSynthetic())
            .filter(SessionEvent::isRootEvent)
            .count();

        return rootEventCount > maxEvents;
    }
}
