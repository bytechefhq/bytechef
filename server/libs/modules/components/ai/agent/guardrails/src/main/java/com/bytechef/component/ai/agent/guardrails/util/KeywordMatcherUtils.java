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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public final class KeywordMatcherUtils {

    private static final int PATTERN_CACHE_MAX_SIZE = 1024;

    private static final Cache<CacheKey, Pattern> PATTERN_CACHE = Caffeine.newBuilder()
        .maximumSize(PATTERN_CACHE_MAX_SIZE)
        .build();

    private KeywordMatcherUtils() {
    }

    public static KeywordMatchResult match(String content, List<String> keywords, boolean caseSensitive) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return new KeywordMatchResult(false, Collections.emptyList());
        }

        CharSequence bounded = RegexParserUtils.bounded(content);

        List<String> matchedKeywords = new ArrayList<>();
        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) {
                continue;
            }

            Pattern pattern = patternFor(keyword, caseSensitive);

            try {
                Matcher matcher = pattern.matcher(bounded);

                if (matcher.find()) {
                    matchedKeywords.add(keyword);
                }
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(
                    new RegexParserUtils.RegexExecutionLimitException(
                        "keyword '" + keyword + "': " + exception.getMessage(), exception));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return new KeywordMatchResult(!matchedKeywords.isEmpty(), matchedKeywords);
    }

    private static Pattern patternFor(String keyword, boolean caseSensitive) {
        return PATTERN_CACHE.get(new CacheKey(keyword, caseSensitive), key -> {
            int flags = key.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            String source = "(?U)(?<![\\p{L}\\p{N}_])" + Pattern.quote(key.keyword()) + "(?![\\p{L}\\p{N}_])";

            return Pattern.compile(source, flags);
        });
    }

    public static KeywordMatchResult match(String content, List<String> keywords) {
        return match(content, keywords, false);
    }

    public static List<String> parseKeywords(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }

        String[] raw = commaSeparated.split(",");
        List<String> result = new ArrayList<>(raw.length);

        for (String token : raw) {
            String trimmed = token.trim()
                .replaceAll("\\p{Punct}+$", "");

            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    public static List<String> findMatchedSubstrings(String content, List<String> keywords, boolean caseSensitive) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        CharSequence bounded = RegexParserUtils.bounded(content);

        List<String> matches = new ArrayList<>();
        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) {
                continue;
            }

            Pattern pattern = patternFor(keyword, caseSensitive);

            try {
                Matcher matcher = pattern.matcher(bounded);

                while (matcher.find()) {
                    matches.add(matcher.group());
                }
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(
                    new RegexParserUtils.RegexExecutionLimitException(
                        "keyword '" + keyword + "': " + exception.getMessage(), exception));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return List.copyOf(matches);
    }

    public static String mask(String content, List<String> keywords, boolean caseSensitive) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return content;
        }

        String result = content;

        // Each keyword pays its own RegexParserUtils.bounded() budget. This is intentional: collapsing the
        // budget across the whole keyword list would mean a long keyword list could starve later entries
        // even on small inputs. Keep per-keyword accounting.
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty()) {
                String mask = "*".repeat(keyword.length());
                Pattern pattern = patternFor(keyword, caseSensitive);

                result = pattern.matcher(RegexParserUtils.bounded(result))
                    .replaceAll(mask);
            }
        }

        return result;
    }

    public static String mask(String content, List<String> keywords) {
        return mask(content, keywords, false);
    }

    public record KeywordMatchResult(boolean matched, List<String> matchedKeywords) {

        public KeywordMatchResult {
            matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
        }
    }

    private record CacheKey(String keyword, boolean caseSensitive) {
    }
}
