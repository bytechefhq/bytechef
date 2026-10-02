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

package com.bytechef.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PublishedInputSpansTest {

    @Test
    void testReadsBackWhatWasPublishedUnderTheKey() {
        SensitiveSpan span = SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 3, 14);
        Map<String, Object> context = new HashMap<>();

        context.put(PublishedInputSpans.CONTEXT_KEY, List.of(span));

        assertThat(PublishedInputSpans.from(context)).containsExactly(span);
    }

    @Test
    void testIsEmptyWhenNothingWasPublished() {
        assertThat(PublishedInputSpans.from(Map.of())).isEmpty();
    }

    @Test
    void testIgnoresAForeignValueUnderTheKeyRatherThanThrowing() {
        Map<String, Object> context = new HashMap<>();

        context.put(PublishedInputSpans.CONTEXT_KEY, "not a list");

        assertThat(PublishedInputSpans.from(context)).isEmpty();

        context.put(PublishedInputSpans.CONTEXT_KEY, List.of("not a span"));

        assertThat(PublishedInputSpans.from(context)).isEmpty();
    }
}
