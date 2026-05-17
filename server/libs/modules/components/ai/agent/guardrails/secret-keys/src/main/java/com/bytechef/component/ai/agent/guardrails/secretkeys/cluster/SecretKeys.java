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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS_BALANCED;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS_PERMISSIVE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PERMISSIVENESS_STRICT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.SecretMatch;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
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
import java.util.stream.Collectors;

/**
 * `Secret Keys` detects API tokens and credential shapes in the input. It runs in the **preflight stage** so the
 * matched substrings are masked before the LLM sees them.
 *
 * @author Ivica Cardic
 */
public final class SecretKeys {

    public static final String ALLOWED_FILE_EXTENSIONS = "allowedFileExtensions";

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("secretKeysCheck")
            .title("Secret Keys")
            .description("Flags the input if known secret-key / API-credential shapes are detected.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(sharedProperties())
            .object(() -> new PreflightCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, resolveConfig(context));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolveConfig(context)));
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

                @Override
                public String apply(String text, GuardrailContext context) {
                    return maskInline(text, resolveConfig(context), context.context());
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolveConfig(context)));
                }
            });
    }

    private SecretKeys() {
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            VALIDATE_INPUT_PROPERTY,
            VALIDATE_OUTPUT_PROPERTY,
            string(PERMISSIVENESS)
                .label("Permissiveness")
                .description("How aggressively to scan for secrets.")
                .options(
                    option(
                        "Strict (most coverage, more false positives)", PERMISSIVENESS_STRICT,
                        "Catches the most — well-known provider tokens, random-looking strings, and generic 'apikey=...' / 'secret=...' assignments — and produces more false positives."),
                    option(
                        "Balanced (default)", PERMISSIVENESS_BALANCED,
                        "Catches well-known provider tokens plus random-looking strings."),
                    option(
                        "Permissive (highest bar)", PERMISSIVENESS_PERMISSIVE,
                        "Catches well-known provider tokens plus random-looking strings at the highest bar (≥30 chars, "
                            + "≥4.0 bits of entropy/char), trading recall for the lowest false-positive rate."))
                .defaultValue(PERMISSIVENESS_BALANCED),
            array(ALLOWED_FILE_EXTENSIONS)
                .label("Allowed File Extensions")
                .description(
                    "Skip detection inside markdown fenced code blocks tagged with these languages "
                        + "(e.g. py, js, ts). Detection still runs outside the fences.")
                .items(string())
                .required(false)
        };
    }

    private static Permissiveness levelOf(Parameters params) {
        String raw = params.getString(PERMISSIVENESS, PERMISSIVENESS_BALANCED);

        try {
            return Permissiveness.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Unknown Secret Keys permissiveness '" + raw + "'. Expected one of " +
                    Arrays.toString(Permissiveness.values()) + ".",
                exception);
        }
    }

    private static ResolvedConfig resolveConfig(GuardrailContext context) {
        Parameters inputParameters = context.inputParameters();

        return new ResolvedConfig(
            levelOf(inputParameters),
            List.copyOf(inputParameters.getList(ALLOWED_FILE_EXTENSIONS, String.class, List.of())));
    }

    private static Optional<Violation> applyCheck(String text, ResolvedConfig config) {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            text, config.permissiveness(), List.of(), config.allowedFileExtensions());

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        List<String> values = matches.stream()
            .map(SecretMatch::value)
            .distinct()
            .toList();

        ArrayList<String> providerTypes = matches.stream()
            .map(SecretMatch::type)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

        return Optional.of(
            Violation.ofMatches("secretKeysCheck", values, Map.of("providerTypes", providerTypes)));
    }

    private static String maskInline(String text, ResolvedConfig config, Context context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Map<String, List<String>> entities = collectMaskEntities(text, config);

        if (entities.isEmpty()) {
            return text;
        }

        MaskEntityMapUtils maskEntityMap = new MaskEntityMapUtils(context);

        maskEntityMap.merge(entities);

        return maskEntityMap.applyTo(text);
    }

    private static Map<String, List<String>> collectMaskEntities(String text, ResolvedConfig config) {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            text, config.permissiveness(), List.of(), config.allowedFileExtensions());

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

    private record ResolvedConfig(Permissiveness permissiveness, List<String> allowedFileExtensions) {
    }
}
