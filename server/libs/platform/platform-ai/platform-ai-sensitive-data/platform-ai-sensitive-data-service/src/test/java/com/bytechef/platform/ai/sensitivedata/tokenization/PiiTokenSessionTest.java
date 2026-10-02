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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class PiiTokenSessionTest {

    @Test
    void testTheSameValueAlwaysGetsTheSameToken() {
        PiiTokenSession session = PiiTokenSession.create();

        String first = session.tokenFor("EMAIL", "bob@acme.io");
        String second = session.tokenFor("EMAIL", "bob@acme.io");

        assertThat(first).isEqualTo(second);
        assertThat(session.size()).isEqualTo(1);
    }

    @Test
    void testDifferentValuesGetDifferentTokens() {
        PiiTokenSession session = PiiTokenSession.create();

        String bob = session.tokenFor("EMAIL", "bob@acme.io");
        String alice = session.tokenFor("EMAIL", "alice@acme.io");

        assertThat(bob).isNotEqualTo(alice);
        assertThat(session.size()).isEqualTo(2);
    }

    @Test
    void testRestoreSubstitutesEveryKnownToken() {
        PiiTokenSession session = PiiTokenSession.create();

        String bob = session.tokenFor("EMAIL", "bob@acme.io");
        String alice = session.tokenFor("EMAIL", "alice@acme.io");

        assertThat(session.restore("forward " + bob + " to " + alice))
            .isEqualTo("forward bob@acme.io to alice@acme.io");
    }

    @Test
    void testRestoreLeavesAnUnknownTokenUntouched() {
        PiiTokenSession session = PiiTokenSession.create();

        String text = "see [PII_EMAIL_9_" + session.sessionId() + "]";

        assertThat(session.restore(text)).isEqualTo(text);
    }

    @Test
    void testRestoreLeavesAForeignSessionTokenUntouched() {
        PiiTokenSession session = PiiTokenSession.create();
        PiiTokenSession other = PiiTokenSession.create();

        String foreign = other.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.restore("see " + foreign)).isEqualTo("see " + foreign);
    }

    @Test
    void testRestoreWithUnresolvedCountReportsAForeignTokenEvenWhenThisSessionMintedNothing() {
        PiiTokenSession session = PiiTokenSession.create();
        PiiTokenSession other = PiiTokenSession.create();

        String foreign = other.tokenFor("EMAIL", "bob@acme.io");

        assertThat(session.size()).isZero();

        PiiTokenSession.RestoreResult result = session.restoreWithUnresolvedCount("see " + foreign);

        assertThat(result.unresolvedCount()).isEqualTo(1);
        assertThat(result.text()).isEqualTo("see " + foreign);
    }

    @Test
    void testCloseClearsTheMapping() {
        PiiTokenSession session = PiiTokenSession.create();

        String token = session.tokenFor("EMAIL", "bob@acme.io");

        session.close();

        assertThat(session.size()).isZero();
        assertThat(session.restore("see " + token)).isEqualTo("see " + token);
    }

    @Test
    void testSessionsGetDistinctIds() {
        Set<String> ids = new HashSet<>();

        for (int attempt = 0; attempt < 50; attempt++) {
            ids.add(PiiTokenSession.create()
                .sessionId());
        }

        assertThat(ids).hasSizeGreaterThan(1);
    }

    @Test
    void testConcurrentMintingKeepsOneTokenPerValue() throws Exception {
        PiiTokenSession session = PiiTokenSession.create();

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        try {
            List<Future<String>> futures = new ArrayList<>();

            for (int attempt = 0; attempt < 200; attempt++) {
                futures.add(executorService.submit(() -> session.tokenFor("EMAIL", "bob@acme.io")));
            }

            Set<String> minted = ConcurrentHashMap.newKeySet();

            for (Future<String> future : futures) {
                minted.add(future.get());
            }

            assertThat(minted).hasSize(1);
            assertThat(session.size()).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void testTokensExportsTheMintedMapAsACopy() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        Map<String, String> tokens = session.tokens();

        assertThat(tokens).containsExactly(Map.entry(token, "bob@acme.io"));

        session.close();

        assertThat(tokens)
            .as("a caller-held map must survive the session that minted it")
            .containsKey(token);
    }

    @Test
    void testRehydratedSessionRestoresTheTokensItWasGiven() {
        PiiTokenSession original = PiiTokenSession.create();
        String token = original.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        PiiTokenSession rehydrated = PiiTokenSession.rehydrate(original.sessionId(), original.tokens());

        assertThat(rehydrated.restore("mail " + token)).isEqualTo("mail bob@acme.io");
        assertThat(rehydrated.sessionId()).isEqualTo(original.sessionId());
    }

    @Test
    void testRehydratedSessionMintsTheSameTokenForAValueItAlreadyHolds() {
        PiiTokenSession original = PiiTokenSession.create();
        String token = original.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        PiiTokenSession rehydrated = PiiTokenSession.rehydrate(original.sessionId(), original.tokens());

        assertThat(rehydrated.tokenFor("EMAIL_ADDRESS", "bob@acme.io"))
            .as("cross-turn coherence: the model must see one person, not two")
            .isEqualTo(token);
    }

    @Test
    void testRehydratedSessionResumesOrdinalsAboveTheHighestItHolds() {
        PiiTokenSession original = PiiTokenSession.create();

        original.tokenFor("EMAIL_ADDRESS", "bob@acme.io");
        original.tokenFor("EMAIL_ADDRESS", "alice@acme.io");

        PiiTokenSession rehydrated = PiiTokenSession.rehydrate(original.sessionId(), original.tokens());

        String third = rehydrated.tokenFor("EMAIL_ADDRESS", "carol@acme.io");

        assertThat(original.tokens()).doesNotContainKey(third);
        assertThat(rehydrated.restore(third)).isEqualTo("carol@acme.io");
        assertThat(rehydrated.restore("mail " + original.tokenFor("EMAIL_ADDRESS", "bob@acme.io")))
            .isEqualTo("mail bob@acme.io");
    }

    @Test
    void testRehydratingAnEmptyMapBehavesLikeAFreshSession() {
        PiiTokenSession rehydrated = PiiTokenSession.rehydrate("abcd", Map.of());

        assertThat(rehydrated.size()).isZero();
        assertThat(rehydrated.tokenFor("EMAIL_ADDRESS", "bob@acme.io")).contains("_1_");
    }

    @Test
    void testRehydrateIgnoresAnEntryWhoseKeyIsNotAToken() {
        PiiTokenSession rehydrated = PiiTokenSession.rehydrate("abcd", Map.of("not-a-token", "bob@acme.io"));

        assertThat(rehydrated.size()).isZero();
        assertThat(rehydrated.restore("not-a-token")).isEqualTo("not-a-token");
    }
}
