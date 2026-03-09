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

package com.bytechef.component.ai.agent.guardrails.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for matching keywords in content.
 *
 * @author Ivica Cardic
 */
public class KeywordMatcher {

    private KeywordMatcher() {
    }

    /**
     * Match keywords in the given content.
     *
     * @param content  the content to scan
     * @param keywords the keywords to look for
     * @return the result containing whether any keywords matched and which ones
     */
    public static KeywordMatchResult match(String content, List<String> keywords) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return new KeywordMatchResult(false, Collections.emptyList());
        }

        String lowerContent = content.toLowerCase();
        List<String> matchedKeywords = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && lowerContent.contains(keyword.toLowerCase())) {
                matchedKeywords.add(keyword);
            }
        }

        return new KeywordMatchResult(!matchedKeywords.isEmpty(), matchedKeywords);
    }

    /**
     * Mask matched keywords in the content.
     *
     * @param content  the content to mask
     * @param keywords the keywords to mask
     * @return the masked content
     */
    public static String mask(String content, List<String> keywords) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return content;
        }

        String result = content;

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty()) {
                String mask = "*".repeat(keyword.length());
                result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(keyword), mask);
            }
        }

        return result;
    }

    /**
     * Result of a keyword matching operation.
     *
     * @param matched         whether any keywords were matched
     * @param matchedKeywords the list of keywords that were matched
     */
    public record KeywordMatchResult(boolean matched, List<String> matchedKeywords) {

        public KeywordMatchResult {
            matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
        }
    }
}
