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

package com.bytechef.component.ai.universal.text.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class MaskSpansTest {

    @Test
    void testKeywordSpansStayAlignedAcrossNonLengthPreservingCaseFolding() {
        String text = "İstanbul falcon nest";

        List<SensitiveSpan> spans = MaskSpans.keywordSpans(text, List.of("falcon"));

        assertThat(spans).hasSize(1);

        SensitiveSpan span = spans.get(0);

        assertThat(text.substring(span.start(), span.end())).isEqualTo("falcon");
    }

    @Test
    void testKeywordSpansMatchEveryOccurrenceCaseInsensitively() {
        String text = "Falcon flies, falcon nests, FALCON soars";

        List<SensitiveSpan> spans = MaskSpans.keywordSpans(text, List.of("falcon"));

        assertThat(spans).hasSize(3)
            .allSatisfy(span -> assertThat(text.substring(span.start(), span.end())).isEqualToIgnoringCase("falcon"));
    }

    @Test
    void testKeywordSpansSkipNullAndBlankKeywords() {
        List<SensitiveSpan> spans = MaskSpans.keywordSpans("falcon nest", Arrays.asList(null, "  ", ""));

        assertThat(spans).isEmpty();
    }
}
