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

package com.bytechef.platform.ai.tool.util;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Applies the per-run component allow-list carried in a Spring AI {@link ToolContext}. A run without the allow-list is
 * unrestricted.
 *
 * @author Ivica Cardic
 */
public final class AllowedComponentNamesUtils {

    public static final String TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY = "allowedComponentNames";

    private static final Logger log = LoggerFactory.getLogger(AllowedComponentNamesUtils.class);

    private static final Pattern NODE_TYPE_PATTERN = Pattern.compile("^([A-Za-z0-9_]+)/v\\d+(?:/.*)?$");

    private AllowedComponentNamesUtils() {
    }

    @SuppressWarnings("unchecked")
    public static @Nullable Set<String> getAllowedComponentNames(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        if (context == null) {
            return null;
        }

        Object value = context.get(TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

        if (value instanceof Set<?> set && !set.isEmpty()) {
            return (Set<String>) set;
        }

        return null;
    }

    public static boolean isAllowed(@Nullable String componentName, @Nullable ToolContext toolContext) {
        Set<String> allowedComponentNames = getAllowedComponentNames(toolContext);

        return allowedComponentNames == null || componentName == null ||
            allowedComponentNames.contains(componentName);
    }

    public static <T> List<T> filter(
        List<T> items, Function<T, @Nullable String> componentNameFunction, @Nullable ToolContext toolContext) {

        Set<String> allowedComponentNames = getAllowedComponentNames(toolContext);

        if (allowedComponentNames == null) {
            return items;
        }

        List<T> filteredItems = new ArrayList<>();

        for (T item : items) {
            String componentName = componentNameFunction.apply(item);

            if (componentName == null || allowedComponentNames.contains(componentName)) {
                filteredItems.add(item);
            }
        }

        return filteredItems;
    }

    public static List<String> getDisallowedComponentNames(
        @Nullable String definition, @Nullable ToolContext toolContext,
        ComponentDefinitionService componentDefinitionService) {

        Set<String> allowedComponentNames = getAllowedComponentNames(toolContext);

        if (allowedComponentNames == null || definition == null || definition.isBlank()) {
            return List.of();
        }

        Object definitionObject;

        try {
            definitionObject = JsonUtils.read(definition);
        } catch (RuntimeException runtimeException) {
            if (log.isDebugEnabled()) {
                log.debug("Workflow definition is not valid JSON; leaving it to the workflow validation",
                    runtimeException);
            }

            return List.of();
        }

        Set<String> referencedComponentNames = new LinkedHashSet<>();

        collectReferencedComponentNames(definitionObject, referencedComponentNames);

        List<String> disallowedComponentNames = new ArrayList<>();

        for (String componentName : referencedComponentNames) {
            if (allowedComponentNames.contains(componentName)) {
                continue;
            }

            Optional<ComponentDefinition> componentDefinitionOptional =
                componentDefinitionService.fetchComponentDefinition(componentName, null);

            if (componentDefinitionOptional.isPresent()) {
                ComponentDefinition componentDefinition = componentDefinitionOptional.get();

                if (componentDefinition.isConnectionRequired()) {
                    disallowedComponentNames.add(componentName);
                }
            }
        }

        return disallowedComponentNames;
    }

    public static String getNotAllowedMessage(Collection<String> componentNames) {
        String joinedComponentNames = String.join(", ", componentNames);

        if (componentNames.size() == 1) {
            return "This chat can't use " + joinedComponentNames +
                ": it isn't one of the integrations available to this user.";
        }

        return "This chat can't use " + joinedComponentNames +
            ": they aren't among the integrations available to this user.";
    }

    private static void collectReferencedComponentNames(@Nullable Object node, Set<String> componentNames) {
        if (node instanceof Map<?, ?> map) {
            if (map.get("type") instanceof String type) {
                Matcher matcher = NODE_TYPE_PATTERN.matcher(type);

                if (matcher.matches()) {
                    componentNames.add(matcher.group(1));
                }
            }

            for (Object value : map.values()) {
                collectReferencedComponentNames(value, componentNames);
            }
        } else if (node instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectReferencedComponentNames(item, componentNames);
            }
        }
    }
}
