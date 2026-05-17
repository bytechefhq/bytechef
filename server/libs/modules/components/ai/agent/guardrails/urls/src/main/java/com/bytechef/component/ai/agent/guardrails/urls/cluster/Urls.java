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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.UrlDetectorUtils;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetectorUtils.UrlMatch;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetectorUtils.UrlPolicy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, policyOf(context.inputParameters()));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return maskEntities(text, policyOf(context.inputParameters()));
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("urlsSanitize")
            .title("URLs")
            .description("Detects URLs outside the allowlist.")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedSanitizeProperties())
            .object(() -> new PreflightSanitizerFunction() {

                @Override
                public String apply(String text, GuardrailContext context) {
                    return maskText(text, policyOf(context.inputParameters()));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return maskEntities(text, policyOf(context.inputParameters()));
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
            VALIDATE_INPUT_PROPERTY,
            VALIDATE_OUTPUT_PROPERTY,
            array(ALLOWED_URLS)
                .label("Allowed URLs")
                .description("Allowlist of URLs or host names that are permitted")
                .items(string()),
            array(ALLOWED_SCHEMES)
                .label("Allowed Schemes")
                .description("Which URL schemes are permitted; URLs using any other scheme are " +
                    (sanitize ? "masked." : "flagged."))
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
                .description(
                    "Extends each entry in 'Allowed URLs' to cover its subdomains. When on, allowing " +
                        "'example.com' also allows 'api.example.com' and 'staging.api.example.com'. " +
                        "When off, only the exact host matches.")
                .defaultValue(true)
        };
    }

    private static UrlPolicy policyOf(Parameters parameters) {
        return new UrlPolicy(
            Objects.requireNonNullElse(parameters.getList(ALLOWED_URLS, String.class), List.of()),
            Objects.requireNonNullElse(
                parameters.getList(ALLOWED_SCHEMES, String.class), List.of("https", "http")),
            parameters.getBoolean(BLOCK_USERINFO, true), parameters.getBoolean(ALLOW_SUBDOMAIN, true));
    }

    private static Optional<Violation> applyCheck(String text, UrlPolicy policy) {
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(text, policy);

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        List<String> blocked = matches.stream()
            .map(UrlMatch::url)
            .toList();

        return Optional.of(Violation.ofMatches("urlsCheck", blocked));
    }

    private static String maskText(String text, UrlPolicy policy) {
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(text, policy);

        return UrlDetectorUtils.mask(text, matches);
    }

    private static MaskResult maskEntities(String text, UrlPolicy policy) {
        if (text == null || text.isEmpty()) {
            return MaskResult.unchanged();
        }

        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(text, policy);

        if (matches.isEmpty()) {
            return MaskResult.unchanged();
        }

        List<String> values = new ArrayList<>(matches.size());

        for (UrlMatch match : matches) {
            String url = match.url();

            if (url != null && !url.isEmpty()) {
                values.add(url);
            }
        }

        if (values.isEmpty()) {
            return MaskResult.unchanged();
        }

        return MaskResult.entities(Map.of("URL", values));
    }
}
