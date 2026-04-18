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

package com.bytechef.component.ai.agent.guardrails.urls.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.ALLOWED_SCHEMES;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.ALLOWED_URLS;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.ALLOW_SUBDOMAIN;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.BLOCK_USERINFO;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetector;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetector.UrlMatch;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetector.UrlPolicy;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rule-based URL policy enforcement. Extracts URLs from text and blocks any host matching a configured deny list (or
 * missing from an allow list). Used both as a check and as a sanitizer — the sanitize variant redacts disallowed URLs
 * rather than raising a violation.
 *
 * @author Ivica Cardic
 */
public final class Urls {

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("urlsCheck")
            .title("URLs")
            .description("Flags URLs outside the allowlist.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(sharedCheckProperties())
            .object(() -> new PreflightCheckFunction() {

                // Reference-equality cache of the resolved UrlPolicy per GuardrailContext. Policy extraction is
                // cheap but the two per-pass lookups (apply + mask) duplicate it needlessly; the cache matches the
                // pattern used by Pii/SecretKeys for consistency and makes per-request resolution trivially clear
                // to future maintainers.
                private final AtomicReference<CachedPolicy> policyCache = new AtomicReference<>();

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, resolvePolicy(context, policyCache));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolvePolicy(context, policyCache)));
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("urlsSanitize")
            .title("URLs")
            .description("Masks URLs outside the allowlist with <URL>.")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedSanitizeProperties())
            .object(() -> new PreflightSanitizerFunction() {

                private final AtomicReference<CachedPolicy> policyCache = new AtomicReference<>();

                @Override
                public String apply(String text, GuardrailContext context) {
                    // Retained for the GuardrailSanitizerFunction contract; the advisor never invokes this for
                    // PreflightMasking sanitizers (mask() produces Entities or Unchanged; Unchanged does not fall
                    // back to apply()). Kept wired so direct callers still get the expected redacted output.
                    return maskText(text, resolvePolicy(context, policyCache));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolvePolicy(context, policyCache)));
                }
            });
    }

    private Urls() {
    }

    private static Property[] sharedCheckProperties() {
        return buildSharedProperties(false);
    }

    private static Property[] sharedSanitizeProperties() {
        return buildSharedProperties(true);
    }

    private static Property[] buildSharedProperties(boolean sanitize) {
        return new Property[] {
            array(ALLOWED_URLS)
                .label("Block All URLs Except")
                .description("URLs (host names) permitted to appear.")
                .items(string()),
            array(ALLOWED_SCHEMES)
                .label("Allowed Schemes")
                .description("Which URL schemes are permitted.")
                .items(string())
                .options(List.of(
                    option("https", "https"),
                    option("http", "http"),
                    option("ftp", "ftp"),
                    option("data", "data"),
                    option("javascript", "javascript"),
                    option("vbscript", "vbscript"),
                    option("mailto", "mailto")))
                .defaultValue("https", "http"),
            bool(BLOCK_USERINFO)
                .label(sanitize ? "Sanitize Userinfo" : "Block Userinfo")
                .description(sanitize
                    ? "Mask URLs that contain user credentials (user:pass@host)."
                    : "Block URLs that contain user credentials (user:pass@host).")
                .defaultValue(true),
            bool(ALLOW_SUBDOMAIN)
                .label("Allow Subdomain")
                .description("When on, subdomains of allowlisted hosts are also permitted.")
                .defaultValue(true),
            GuardrailProperties.failMode()
        };
    }

    private static UrlPolicy policyOf(Parameters params) {
        return new UrlPolicy(
            Objects.requireNonNullElse(params.getList(ALLOWED_URLS, String.class), List.of()),
            Objects.requireNonNullElse(
                params.getList(ALLOWED_SCHEMES, String.class), List.of("https", "http")),
            params.getBoolean(BLOCK_USERINFO, true),
            params.getBoolean(ALLOW_SUBDOMAIN, true));
    }

    private static UrlPolicy resolvePolicy(GuardrailContext context, AtomicReference<CachedPolicy> cache) {
        CachedPolicy cached = cache.get();

        if (cached != null && cached.context == context) {
            return cached.policy();
        }

        UrlPolicy fresh = policyOf(context.inputParameters());

        cache.set(new CachedPolicy(context, fresh));

        return fresh;
    }

    private static Optional<Violation> applyCheck(String text, UrlPolicy policy) {
        List<UrlMatch> matches = UrlDetector.detectViolations(text, policy);

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        List<String> blocked = matches.stream()
            .map(UrlMatch::url)
            .toList();

        // Raw URLs are carried on matchedSubstrings only. Do NOT duplicate them into info: the advisor's public-view
        // projection reduces matchedSubstrings to matchCount so sensitive values never cross the advisor boundary;
        // placing the same list in info would bypass that scrubbing because toPublicView copies info verbatim.
        return Optional.of(Violation.ofMatches("urlsCheck", blocked));
    }

    private static String maskText(String text, UrlPolicy policy) {
        List<UrlMatch> matches = UrlDetector.detectViolations(text, policy);

        return UrlDetector.mask(text, matches);
    }

    private static Map<String, List<String>> collectMaskEntities(String text, UrlPolicy policy) {
        List<UrlMatch> matches = UrlDetector.detectViolations(text, policy);

        if (matches.isEmpty()) {
            return Map.of();
        }

        List<String> blocked = matches.stream()
            .map(UrlMatch::url)
            .distinct()
            .toList();

        return Map.of("URL", blocked);
    }

    private record CachedPolicy(GuardrailContext context, UrlPolicy policy) {
    }
}
