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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils.PiiMatch;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.SecretMatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Smoke-tests every real detector under parallel load to catch accidental static caches or shared {@code Matcher}
 * instances. The advisor's per-check concurrency tests use stateless lambdas; if a future refactor adds a static cache
 * to one of these detectors, those tests would still pass but production would experience cross-request state leak.
 * This test pins the per-call independence at the detector level.
 *
 * @author Ivica Cardic
 */
class DetectorConcurrencyTest {

    private static final int THREADS = 50;
    private static final long TIMEOUT_SECONDS = 30;

    @Test
    void testPiiDetectorIsThreadSafeAcrossConcurrentInvocations() throws Exception {
        String input = "Contact: user@example.com or call 555-123-4567";
        List<PiiMatch> expected = PiiDetectorUtils.detect(input, PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        List<Callable<List<PiiMatch>>> tasks = new ArrayList<>(THREADS);

        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> PiiDetectorUtils.detect(input, PiiDetectorUtils.DEFAULT_PII_PATTERNS));
        }

        List<List<PiiMatch>> results = runAll(tasks);

        for (List<PiiMatch> result : results) {
            assertThat(result)
                .as("every concurrent PII detection must produce the same matches as the serial baseline")
                .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void testSecretKeyDetectorIsThreadSafeAcrossConcurrentInvocations() throws Exception {
        String input = "key: sk-AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOp";
        SecretKeyDetectorUtils.Permissiveness level = SecretKeyDetectorUtils.Permissiveness.BALANCED;
        List<SecretMatch> expected = SecretKeyDetectorUtils.detect(input, level);

        List<Callable<List<SecretMatch>>> tasks = new ArrayList<>(THREADS);

        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> SecretKeyDetectorUtils.detect(input, level));
        }

        List<List<SecretMatch>> results = runAll(tasks);

        for (List<SecretMatch> result : results) {
            assertThat(result)
                .as("every concurrent secret-key detection must produce the same matches as the serial baseline")
                .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void testKeywordMatcherIsThreadSafeAcrossConcurrentInvocations() throws Exception {
        String input = "the forbidden word appears in this content";
        List<String> keywords = List.of("forbidden", "banned", "blocked");
        KeywordMatcherUtils.KeywordMatchResult expected =
            KeywordMatcherUtils.match(input, keywords);

        List<Callable<KeywordMatcherUtils.KeywordMatchResult>> tasks = new ArrayList<>(THREADS);

        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> KeywordMatcherUtils.match(input, keywords));
        }

        List<KeywordMatcherUtils.KeywordMatchResult> results = runAll(tasks);

        for (KeywordMatcherUtils.KeywordMatchResult result : results) {
            assertThat(result.matched())
                .as("every concurrent keyword match must agree with the serial baseline")
                .isEqualTo(expected.matched());
            assertThat(result.matchedKeywords())
                .as("every concurrent keyword match must produce the same matched-keyword list")
                .containsExactlyElementsOf(expected.matchedKeywords());
        }
    }

    @Test
    void testUrlDetectorIsThreadSafeAcrossConcurrentInvocations() throws Exception {
        String input = "Visit https://example.com or http://evil.com today";
        UrlDetectorUtils.UrlPolicy policy = new UrlDetectorUtils.UrlPolicy(
            List.of(), List.of("https", "http"), false, false);
        List<UrlDetectorUtils.UrlMatch> expected = UrlDetectorUtils.detectViolations(input, policy);

        List<Callable<List<UrlDetectorUtils.UrlMatch>>> tasks = new ArrayList<>(THREADS);

        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> UrlDetectorUtils.detectViolations(input, policy));
        }

        List<List<UrlDetectorUtils.UrlMatch>> results = runAll(tasks);

        for (List<UrlDetectorUtils.UrlMatch> result : results) {
            assertThat(result)
                .as("every concurrent URL detection must produce the same matches as the serial baseline")
                .containsExactlyElementsOf(expected);
        }
    }

    private static <T> List<T> runAll(List<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            List<Future<T>> futures = executor.invokeAll(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<T> results = new ArrayList<>(futures.size());

            for (Future<T> future : futures) {
                results.add(future.get());
            }

            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
