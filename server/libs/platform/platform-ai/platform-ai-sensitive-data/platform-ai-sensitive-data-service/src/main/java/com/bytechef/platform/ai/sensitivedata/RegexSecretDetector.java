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

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RegexSecretDetector implements SensitiveDataDetector {

    private static final String CATEGORY = "SECRET";

    private static final String STRIPE_PUBLISHABLE_KEY_TYPE = "STRIPE_PUBLISHABLE_KEY";

    private final List<SecretPatternCatalog.SecretPattern> patterns = SecretPatternCatalog.ALL.stream()
        .filter(secretPattern -> !secretPattern.type()
            .equals(STRIPE_PUBLISHABLE_KEY_TYPE))
        .toList();

    @Override
    public String name() {
        return "regex-secret";
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        return detect(text, MatchDeadline.unbounded());
    }

    @Override
    public List<SensitiveSpan> detect(String text, MatchDeadline deadline) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> spans = new ArrayList<>();

        for (SecretPatternCatalog.SecretPattern secretPattern : patterns) {
            Matcher matcher = secretPattern.pattern()
                .matcher(deadline.bound(text));

            while (matcher.find()) {
                spans.add(
                    new SensitiveSpan(
                        SensitiveKind.SECRET, CATEGORY, matcher.start(), matcher.end(), secretPattern.score()));
            }
        }

        return spans;
    }
}
