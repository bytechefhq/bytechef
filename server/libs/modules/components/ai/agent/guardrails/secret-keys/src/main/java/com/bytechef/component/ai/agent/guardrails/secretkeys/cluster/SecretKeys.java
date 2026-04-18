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

package com.bytechef.component.ai.agent.guardrails.secretkeys.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CUSTOM_REGEXES;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS_BALANCED;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS_PERMISSIVE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS_STRICT;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.RegexParser;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector.SecretMatch;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public final class SecretKeys {

    /** Array property name for file-extension allowlist (markdown code-fence language tags). */
    public static final String ALLOWED_FILE_EXTENSIONS = "allowedFileExtensions";

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("secretKeysCheck")
            .title("Secret Keys")
            .description("Flags the input if known secret-key / API-credential shapes are detected.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(sharedProperties())
            .object(() -> new PreflightCheckFunction() {

                // Reference-equality cache of the compiled config per GuardrailContext. Parallel to Pii.ofCheck:
                // user custom regexes compile once per advisor pass instead of once per method invocation.
                private final AtomicReference<CachedConfig> configCache = new AtomicReference<>();

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, resolveConfig(context, configCache));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolveConfig(context, configCache)));
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("secretKeysSanitize")
            .title("Secret Keys")
            .description("Masks detected secret keys / API credentials with <TYPE> placeholders.")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedProperties())
            .object(() -> new PreflightSanitizerFunction() {

                private final AtomicReference<CachedConfig> configCache = new AtomicReference<>();

                @Override
                public String apply(String text, GuardrailContext context) {
                    // Retained for the GuardrailSanitizerFunction contract; the advisor never invokes this for
                    // PreflightMasking sanitizers because mask() returns Entities (or Unchanged) and Unchanged does
                    // not fall back to apply(). Kept wired for direct-caller parity.
                    return maskText(text, resolveConfig(context, configCache));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolveConfig(context, configCache)));
                }
            });
    }

    private SecretKeys() {
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            string(PERMISSIVENESS)
                .label("Permissiveness")
                .description("STRICT = most aggressive; BALANCED = named providers + entropy; "
                    + "PERMISSIVE = named providers only.")
                .options(
                    option("Strict", PERMISSIVENESS_STRICT),
                    option("Balanced", PERMISSIVENESS_BALANCED),
                    option("Permissive", PERMISSIVENESS_PERMISSIVE))
                .defaultValue(PERMISSIVENESS_BALANCED),
            array(CUSTOM_REGEXES)
                .label("Custom Regexes")
                .description("Additional regex patterns to detect (alongside the built-in providers and entropy "
                    + "scanner). Use the /pattern/flags literal form to specify case-insensitivity, e.g. "
                    + "/myapikey-[a-z0-9]{32}/i.")
                .items(string())
                .required(false),
            array(ALLOWED_FILE_EXTENSIONS)
                .label("Allowed File Extensions")
                .description("Skip secret detection inside markdown fenced code blocks tagged with these "
                    + "extensions (e.g. py, js, ts). Useful when the input legitimately contains code samples.")
                .items(string())
                .required(false),
            GuardrailProperties.failMode()
        };
    }

    private static Permissiveness levelOf(Parameters params) {
        String raw = params.getString(PERMISSIVENESS, PERMISSIVENESS_BALANCED);

        try {
            return Permissiveness.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown Secret Keys permissiveness '" + raw + "'. Expected one of "
                    + Arrays.toString(Permissiveness.values()) + ".",
                e);
        }
    }

    private static List<Pattern> customRegexesOf(Parameters params) {
        List<String> rawCustomRegexes = params.getList(CUSTOM_REGEXES, String.class, List.of());
        List<Pattern> compiled = new ArrayList<>(rawCustomRegexes.size());

        for (String raw : rawCustomRegexes) {
            try {
                compiled.add(RegexParser.compile(raw));
            } catch (IllegalArgumentException cause) {
                // Log-once-per-TTL to correlate "every request blocked" to a specific bad regex without spamming the
                // ERROR channel on every request. See Pii.customRegexesOf for parity.
                RegexParser.logCompileErrorOnce("secret-keys", raw, cause);

                // IllegalArgumentException covers both RegexParser's explicit bad-flag rejections and the
                // PatternSyntaxException it re-throws (PatternSyntaxException extends IllegalArgumentException).
                // Surfaced as IllegalArgumentException so the advisor's recordFailure treats it as a configuration
                // error (always fail-closed) regardless of the user's FAIL_MODE setting.
                throw new IllegalArgumentException(
                    "Invalid Secret Keys custom regex '" + raw + "': " + cause.getMessage(), cause);
            }
        }

        return List.copyOf(compiled);
    }

    private static CachedConfig resolveConfig(GuardrailContext context, AtomicReference<CachedConfig> cache) {
        CachedConfig cached = cache.get();

        if (cached != null && cached.context == context) {
            return cached;
        }

        Parameters inputParameters = context.inputParameters();
        CachedConfig fresh = new CachedConfig(
            context, levelOf(inputParameters), customRegexesOf(inputParameters),
            List.copyOf(inputParameters.getList(ALLOWED_FILE_EXTENSIONS, String.class, List.of())));

        cache.set(fresh);

        return fresh;
    }

    private static Optional<Violation> applyCheck(String text, CachedConfig config) {
        List<SecretMatch> matches = SecretKeyDetector.detect(
            text, config.permissiveness(), config.customRegexes(), config.allowedFileExtensions());

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        List<String> values = matches.stream()
            .map(SecretMatch::value)
            .distinct()
            .toList();

        List<String> providerTypes = matches.stream()
            .map(SecretMatch::type)
            .distinct()
            .toList();

        return Optional.of(
            Violation.ofMatches("secretKeysCheck", values, Map.of("providerTypes", providerTypes)));
    }

    private static String maskText(String text, CachedConfig config) {
        List<SecretMatch> matches = SecretKeyDetector.detect(
            text, config.permissiveness(), config.customRegexes(), config.allowedFileExtensions());

        return SecretKeyDetector.mask(text, matches);
    }

    private static Map<String, List<String>> collectMaskEntities(String text, CachedConfig config) {
        List<SecretMatch> matches = SecretKeyDetector.detect(
            text, config.permissiveness(), config.customRegexes(), config.allowedFileExtensions());

        if (matches.isEmpty()) {
            return Map.of();
        }

        Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();

        for (SecretMatch match : matches) {
            grouped.computeIfAbsent(match.type(), key -> new LinkedHashSet<>())
                .add(match.value());
        }

        Map<String, List<String>> result = new LinkedHashMap<>();

        grouped.forEach((type, values) -> result.put(type, new ArrayList<>(values)));

        return result;
    }

    /**
     * Compiled per-request configuration shared between {@code apply}/{@code mask} and {@code preflightMaskEntities} so
     * user custom regexes compile once per advisor pass instead of once per method invocation.
     */
    private record CachedConfig(
        GuardrailContext context, Permissiveness permissiveness, List<Pattern> customRegexes,
        List<String> allowedFileExtensions) {
    }
}
