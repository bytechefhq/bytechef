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

package com.bytechef.platform.ai.sensitivedata;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author Ivica Cardic
 */
public final class CustomPatternEvaluator {

    private CustomPatternEvaluator() {
    }

    public static List<SensitiveSpan> detect(String text, List<CustomPattern> patterns, MatchDeadline deadline) {
        if (text == null || text.isEmpty() || patterns.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> spans = new ArrayList<>();

        for (CustomPattern customPattern : patterns) {
            Matcher matcher = customPattern.pattern()
                .matcher(deadline.bound(text));

            while (matcher.find()) {
                spans.add(
                    new SensitiveSpan(
                        customPattern.kind(), customPattern.type(), matcher.start(), matcher.end(),
                        PiiPatternCatalog.promotedScore(
                            customPattern.contextRule(), customPattern.score(), text, matcher.start(),
                            matcher.end())));
            }
        }

        return spans;
    }
}
