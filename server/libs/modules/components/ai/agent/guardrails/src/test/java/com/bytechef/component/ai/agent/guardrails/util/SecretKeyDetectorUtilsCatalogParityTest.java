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

import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.SecretMatch;
import com.bytechef.platform.ai.sensitivedata.SecretPatternCatalog;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SecretKeyDetectorUtilsCatalogParityTest {

    @Test
    void testDetectsGithubOAuthTokenNowThatGithubPatIsThePlatformSuperset() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "token gho_" + "a".repeat(36) + " here", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("GITHUB_PAT");
    }

    @Test
    void testDetectsSlackRestrictedTokenNowThatSlackTokenIsThePlatformSuperset() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "token xoxr-" + "a".repeat(20) + " here", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("SLACK_TOKEN");
    }

    @Test
    void testDetectsGithubFineGrainedTokenShorterThanTheComponentsOldExactLength() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "token github_pat_" + "A".repeat(30) + " here", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("GITHUB_FINE_GRAINED_PAT");
    }

    @Test
    void testDetectsOpenAiProjectScopedKeyNowThatOpenAiKeyIsThePlatformSuperset() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "token sk-proj-abcdefghij1234567890abcdef end", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("OPENAI_KEY");
    }

    @Test
    void testDetectsStripeRestrictedLiveKeyStillReportedAsStripeKey() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "key rk_live_" + "B".repeat(24) + " done", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("STRIPE_KEY");
    }

    @Test
    void testDetectsStripePublishableKeyStillReportedAsStripeKey() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "key pk_test_" + "A".repeat(20) + " done", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("STRIPE_KEY")
            .doesNotContain("STRIPE_PUBLISHABLE_KEY", "STRIPE_SECRET_KEY");
    }

    @Test
    void testNamedTypesMatchCatalogWithKnownExclusionsAndOverridesApplied() {
        String sample = String.join(
            " ",
            "-----BEGIN RSA PRIVATE KEY-----\nMIIBOgIBAAJBAKj34Gkx...\n-----END RSA PRIVATE KEY-----",
            "AKIAIOSFODNN7EXAMPLE",
            "awsSecretKey=\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\"",
            "ghp_" + "a".repeat(36),
            "github_pat_" + "A".repeat(30),
            "xoxb-" + "a".repeat(20),
            "sk_live_" + "A".repeat(20),
            "pk_test_" + "A".repeat(20),
            "AIza" + "A".repeat(35),
            "sk-" + "a".repeat(24),
            "eyJhbGciOiJIUzI.eyJzdWIiOiIxMjM0.SflKxwRJSMeKKF2QT4");

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(sample, Permissiveness.PERMISSIVE);

        Set<String> catalogTypes = SecretPatternCatalog.ALL.stream()
            .map(SecretPatternCatalog.SecretPattern::type)
            .collect(Collectors.toSet());

        Set<String> expectedComponentTypes = catalogTypes.stream()
            .filter(type -> !type.equals("PEM_PRIVATE_KEY"))
            .filter(type -> !type.equals("STRIPE_SECRET_KEY"))
            .filter(type -> !type.equals("STRIPE_PUBLISHABLE_KEY"))
            .collect(Collectors.toCollection(HashSet::new));

        expectedComponentTypes.add("STRIPE_KEY");

        Set<String> namedTypesReported = matches.stream()
            .map(SecretMatch::type)
            .filter(expectedComponentTypes::contains)
            .collect(Collectors.toSet());

        assertThat(namedTypesReported)
            .as("every catalog type, with known exclusions/overrides applied, must be reachable through the "
                + "component")
            .isEqualTo(expectedComponentTypes);
        assertThat(matches)
            .as("the raw catalog-only types must never be reported by the component's public detect()")
            .extracting(SecretMatch::type)
            .doesNotContain("PEM_PRIVATE_KEY", "STRIPE_SECRET_KEY", "STRIPE_PUBLISHABLE_KEY");
    }
}
