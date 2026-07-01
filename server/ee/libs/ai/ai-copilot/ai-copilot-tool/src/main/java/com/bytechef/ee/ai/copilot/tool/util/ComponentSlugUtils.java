/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool.util;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the "no such component" tool-error message shared by {@code createConnection} and
 * {@code listConnectionsForComponent}.
 *
 * <p>
 * The LLM frequently passes a colloquial name (e.g. {@code "gmail"}) instead of the exact ByteChef component slug
 * ({@code "googleMail"}). Both connection tools used to tolerate that silently — {@code createConnection} fell back to
 * the slug as the button label and {@code listConnectionsForComponent} returned an empty connections envelope — so the
 * bad slug rode through to the client, where the strict {@code GET /component-definitions/{name}/versions/{version}}
 * lookup finally threw {@code IllegalArgumentException} and surfaced as an opaque "Bad Request" toast. This helper
 * turns that into an actionable tool error: the agent reads the candidate slugs (matched on component name OR title, so
 * "gmail" resolves to "googleMail" whose title is "Gmail") and retries with the correct one.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ComponentSlugUtils {

    private static final Logger log = LoggerFactory.getLogger(ComponentSlugUtils.class);

    private static final int MAX_SUGGESTIONS = 5;

    private ComponentSlugUtils() {
    }

    /**
     * Returns the single catalog slug a colloquial {@code componentName} unambiguously resolves to, or {@code null}
     * when zero or 2+ components match.
     */
    public static @Nullable String resolveSingleMatch(
        String componentName, ComponentDefinitionService componentDefinitionService) {

        List<ComponentDefinition> matches = findMatches(componentName, componentDefinitionService);

        ComponentDefinition componentDefinition = matches.getFirst();

        return matches.size() == 1 ? componentDefinition.getName() : null;
    }

    public static String unknownComponentMessage(
        String componentName, ComponentDefinitionService componentDefinitionService) {

        StringBuilder message = new StringBuilder(
            "No component named '" + componentName + "' exists in the ByteChef catalog. componentName must be the " +
                "exact ByteChef component slug, not a colloquial or display name.");

        List<String> candidates = findMatches(componentName, componentDefinitionService)
            .stream()
            .limit(MAX_SUGGESTIONS)
            .map(ComponentSlugUtils::format)
            .toList();

        if (candidates.isEmpty()) {
            message.append(" Use the workflow_editor_agent to look up the correct component slug, then retry.");
        } else {
            message.append(" Did you mean: ")
                .append(String.join(", ", candidates))
                .append("? Retry with the exact slug.");
        }

        return message.toString();
    }

    private static List<ComponentDefinition> findMatches(
        String componentName, ComponentDefinitionService componentDefinitionService) {

        try {
            String query = normalize(componentName);

            if (query.isEmpty()) {
                return List.of();
            }

            return componentDefinitionService.getComponentDefinitions()
                .stream()
                .filter(componentDefinition -> contains(componentDefinition.getName(), query) ||
                    contains(componentDefinition.getTitle(), query))
                .toList();
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to compute component slug matches for '{}'. Reason: {}", componentName, exception.getMessage());

            return List.of();
        }
    }

    private static boolean contains(@Nullable String value, String query) {
        return value != null && normalize(value).contains(query);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private static String format(ComponentDefinition componentDefinition) {
        String name = componentDefinition.getName();
        String title = componentDefinition.getTitle();

        return (title == null || title.isBlank()) ? name : name + " (" + title + ")";
    }
}
