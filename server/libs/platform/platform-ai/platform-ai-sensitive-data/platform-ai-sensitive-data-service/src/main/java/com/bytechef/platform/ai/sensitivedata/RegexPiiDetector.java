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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RegexPiiDetector implements SensitiveDataDetector {

    private final List<PiiPatternCatalog.PiiPattern> patterns;

    public RegexPiiDetector() {
        this(PiiPatternCatalog.curatedDefault());
    }

    public RegexPiiDetector(List<PiiPatternCatalog.PiiPattern> patterns) {
        this.patterns = List.copyOf(patterns);
    }

    @Override
    public String name() {
        return "regex-pii";
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
        for (PiiPatternCatalog.PiiPattern piiPattern : patterns) {
            Matcher matcher = piiPattern.pattern()
                .matcher(deadline.bound(text));
            Predicate<String> validator = piiPattern.validator();

            while (matcher.find()) {
                if (validator != null && !validator.test(matcher.group())) {
                    continue;
                }

                spans.add(
                    new SensitiveSpan(
                        SensitiveKind.PII, piiPattern.type(), matcher.start(), matcher.end(),
                        PiiPatternCatalog.promotedScore(
                            piiPattern.contextRule(), piiPattern.score(), text, matcher.start(),
                            matcher.end())));
            }
        }

        return spans;
    }
}
