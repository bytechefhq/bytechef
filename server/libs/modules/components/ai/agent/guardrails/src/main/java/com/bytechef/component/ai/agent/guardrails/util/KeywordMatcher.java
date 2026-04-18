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
import java.util.regex.Pattern;

/**
 * Utility class for matching keywords in content.
 *
 * @author Ivica Cardic
 */
public final class KeywordMatcher {

    /**
     * Soft cap on the number of cached compiled keyword patterns. A single workflow typically uses fewer than 100
     * distinct keywords; the cap keeps multi-tenant JVMs bounded even when many workflows with disjoint keyword lists
     * run over days/weeks.
     */
    private static final int PATTERN_CACHE_MAX_SIZE = 1024;

    /**
     * LRU cache of compiled keyword patterns, keyed by {@link CacheKey}. Keywords are small strings reused across many
     * requests in a workflow — recompiling the {@link Pattern} per {@link #match(String, List, boolean)} call is pure
     * overhead. The cache is bounded to {@link #PATTERN_CACHE_MAX_SIZE} entries so long-running multi-tenant processes
     * don't accumulate every (keyword, caseSensitive) pair forever.
     *
     * <p>
     * Caffeine is used in preference to {@code Collections.synchronizedMap(LinkedHashMap, accessOrder=true)} because an
     * access-ordered {@code LinkedHashMap} treats every {@code get} as a structural modification, forcing all reads
     * through a single monitor. Caffeine uses sharded read/write buffers and concurrent eviction, so per-keyword
     * lookups on the hot guardrail path do not contend on a global lock.
     */
    private static final Cache<CacheKey, Pattern> PATTERN_CACHE = Caffeine.newBuilder()
        .maximumSize(PATTERN_CACHE_MAX_SIZE)
        .build();

    private KeywordMatcher() {
    }

    public static KeywordMatchResult match(String content, List<String> keywords, boolean caseSensitive) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return new KeywordMatchResult(false, Collections.emptyList());
        }

        List<String> matchedKeywords = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) {
                continue;
            }

            Pattern pattern = patternFor(keyword, caseSensitive);

            if (pattern.matcher(content)
                .find()) {
                matchedKeywords.add(keyword);
            }
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

    private record CacheKey(String keyword, boolean caseSensitive) {
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

    public static String mask(String content, List<String> keywords, boolean caseSensitive) {
        if (content == null || content.isEmpty() || keywords == null || keywords.isEmpty()) {
            return content;
        }

        String result = content;

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty()) {
                String mask = "*".repeat(keyword.length());
                // Use the SAME word-boundary guard as match(...) so mask only replaces whole-word occurrences that
                // match would also flag. Without the guard mask would mask substrings of legitimate words
                // (e.g. mask("programming", ["program"]) would produce "*******ming"), inconsistent with match.
                String boundaryGuarded = "(?U)(?<![\\p{L}\\p{N}_])" + Pattern.quote(keyword) + "(?![\\p{L}\\p{N}_])";
                String regex = caseSensitive
                    ? boundaryGuarded
                    : "(?iu)" + boundaryGuarded;

                result = result.replaceAll(regex, mask);
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
}
