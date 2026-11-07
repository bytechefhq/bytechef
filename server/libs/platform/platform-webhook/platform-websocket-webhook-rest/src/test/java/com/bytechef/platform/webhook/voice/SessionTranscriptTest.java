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

package com.bytechef.platform.webhook.voice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SessionTranscriptTest {

    @Test
    void testEntriesStopAtTheCapAndKeepTheEarliest() {
        SessionTranscript transcript = new SessionTranscript();

        for (int index = 0; index < SessionTranscript.MAX_ENTRIES + 5; index++) {
            if (index % 2 == 0) {
                transcript.user("user " + index);
            } else {
                transcript.assistant("assistant " + index);
            }
        }

        assertThat(transcript.entries()).hasSize(SessionTranscript.MAX_ENTRIES);
        assertThat(transcript.entries()
            .getFirst()).containsEntry("text", "user 0");
        assertThat(transcript.entries()
            .getLast()).containsEntry("text", "assistant " + (SessionTranscript.MAX_ENTRIES - 1));
    }

    @Test
    void testToolCallsStopAtTheCapIndependentlyOfEntries() {
        SessionTranscript transcript = new SessionTranscript();

        for (int index = 0; index < SessionTranscript.MAX_ENTRIES + 5; index++) {
            transcript.toolCall("tool " + index, Map.of(), "result");
        }

        transcript.user("still recorded");

        assertThat(transcript.toolCalls()).hasSize(SessionTranscript.MAX_ENTRIES);
        assertThat(transcript.entries()).singleElement()
            .satisfies(entry -> assertThat(entry).containsEntry("text", "still recorded"));
    }
}
