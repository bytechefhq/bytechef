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

import com.bytechef.platform.ai.sensitivedata.CustomPattern;
import com.bytechef.platform.ai.sensitivedata.CustomPatternEvaluator;
import com.bytechef.platform.ai.sensitivedata.MatchDeadline;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Ivica Cardic
 */
public final class MaskSpans {

    public static final String KEYWORD_CATEGORY = "KEYWORD";
    public static final String CUSTOM_CATEGORY = "CUSTOM";

    private MaskSpans() {
    }

    public static List<SensitiveSpan> keywordSpans(String text, List<String> keywords) {
        List<SensitiveSpan> spans = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }

            Pattern pattern = Pattern.compile(
                Pattern.quote(keyword), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                spans.add(SensitiveSpan.of(SensitiveKind.PII, KEYWORD_CATEGORY, matcher.start(), matcher.end()));
            }
        }

        return spans;
    }

    public static List<SensitiveSpan> customPatternSpans(String text, List<String> patterns, MatchDeadline deadline) {
        List<CustomPattern> compiled = new ArrayList<>(patterns.size());

        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }

            try {
                compiled.add(
                    new CustomPattern(CUSTOM_CATEGORY, Pattern.compile(pattern), SensitiveKind.PII, 1.0, null));
            } catch (PatternSyntaxException patternSyntaxException) {
                throw new IllegalArgumentException("Invalid custom pattern: " + pattern, patternSyntaxException);
            }
        }

        if (compiled.isEmpty()) {
            return List.of();
        }

        return CustomPatternEvaluator.detect(text, compiled, deadline);
    }
}
