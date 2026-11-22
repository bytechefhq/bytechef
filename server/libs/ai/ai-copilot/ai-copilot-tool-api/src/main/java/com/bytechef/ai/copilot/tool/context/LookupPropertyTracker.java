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

package com.bytechef.ai.copilot.tool.context;

import com.bytechef.commons.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
public final class LookupPropertyTracker {

    public static final String TOOL_CONTEXT_LOOKUP_PROPERTY_TRACKER_KEY = "bytechef.agentTool.lookupPropertyTracker";

    private static final Pattern LOOKUP_REQUIRED_PROPERTY = Pattern.compile(
        "\"([A-Za-z_][A-Za-z0-9_]*)\"\\s*:\\s*\\{\\s*\"value\"\\s*:[^{}]*?\"lookupRequired\"\\s*:\\s*true");
    private static final Pattern WORD_BOUNDARY = Pattern.compile("(?<=[a-z0-9])(?=[A-Z])|[^A-Za-z0-9]+");
    private static final Set<String> GENERIC_WORDS = Set.of("id", "ids", "key", "name", "value", "values");
    private static final Set<String> RESOLVING_TOOL_NAMES = Set.of(
        "selectPropertyOption", "selectTriggerPropertyOption");

    private final Set<String> lookupPropertyNames = ConcurrentHashMap.newKeySet();
    private final Set<String> resolvedPropertyNames = ConcurrentHashMap.newKeySet();

    private LookupPropertyTracker() {
    }

    public static void install(Map<String, Object> toolContext) {
        toolContext.put(TOOL_CONTEXT_LOOKUP_PROPERTY_TRACKER_KEY, new LookupPropertyTracker());
    }

    public static void record(
        @Nullable ToolContext toolContext, String toolName, @Nullable String toolInput, @Nullable String toolResult) {

        LookupPropertyTracker tracker = fetch(toolContext);

        if (tracker == null) {
            return;
        }

        if (RESOLVING_TOOL_NAMES.contains(toolName)) {
            String propertyName = readPropertyName(toolInput);

            if (propertyName != null) {
                tracker.resolvedPropertyNames.add(propertyName);
            }

            return;
        }

        if (toolResult == null || !toolResult.contains("lookupRequired")) {
            return;
        }

        Matcher matcher = LOOKUP_REQUIRED_PROPERTY.matcher(toolResult);

        while (matcher.find()) {
            tracker.lookupPropertyNames.add(matcher.group(1));
        }
    }

    public static @Nullable String fetchUnresolvedPropertyMentionedIn(
        @Nullable ToolContext toolContext, List<String> texts) {

        LookupPropertyTracker tracker = fetch(toolContext);

        if (tracker == null) {
            return null;
        }

        String joinedText = " " + String.join(" ", texts)
            .toLowerCase(Locale.ROOT) + " ";

        for (String propertyName : tracker.lookupPropertyNames) {
            if (tracker.resolvedPropertyNames.contains(propertyName)) {
                continue;
            }

            for (String word : toSignificantWords(propertyName)) {
                if (Pattern.compile("[^a-z0-9]" + Pattern.quote(word) + "s?[^a-z0-9]")
                    .matcher(joinedText)
                    .find()) {

                    return propertyName;
                }
            }
        }

        return null;
    }

    private static @Nullable LookupPropertyTracker fetch(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        if (context == null) {
            return null;
        }

        return context.get(TOOL_CONTEXT_LOOKUP_PROPERTY_TRACKER_KEY) instanceof LookupPropertyTracker tracker
            ? tracker : null;
    }

    private static @Nullable String readPropertyName(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return null;
        }

        try {
            return JsonUtils.read(toolInput) instanceof Map<?, ?> input
                && input.get("propertyName") instanceof String propertyName ? propertyName : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static List<String> toSignificantWords(String propertyName) {
        List<String> words = new ArrayList<>();

        for (String word : WORD_BOUNDARY.split(propertyName)) {
            String lowerCaseWord = word.toLowerCase(Locale.ROOT);

            if (lowerCaseWord.length() > 2 && !GENERIC_WORDS.contains(lowerCaseWord)) {
                words.add(lowerCaseWord);
            }
        }

        return words;
    }
}
