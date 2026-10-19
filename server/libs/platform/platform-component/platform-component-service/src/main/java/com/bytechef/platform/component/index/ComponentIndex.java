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

package com.bytechef.platform.component.index;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.ComponentDsl.unifiedApi;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiablePropertyGroup;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoaderKind;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public record ComponentIndex(List<Entry> entries) {

    public ComponentIndex {
        entries = List.copyOf(entries);
    }

    public static final String RESOURCE_PATH = "META-INF/bytechef/component-index.json";

    private static final Logger log = LoggerFactory.getLogger(ComponentIndex.class);

    public static Optional<ComponentIndex> load(ClassLoader classLoader) {
        List<Entry> entries = new ArrayList<>();

        Set<String> seenComponents = new HashSet<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(RESOURCE_PATH);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream inputStream = url.openStream()) {
                    ComponentIndex componentIndex = JsonUtils.read(inputStream, ComponentIndex.class);

                    for (Entry entry : componentIndex.entries()) {
                        if (seenComponents.add(entry.name() + "/" + entry.version())) {
                            entries.add(entry);
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            log.warn(
                "Failed to read component index from classpath; falling back to full component loading", exception);

            return Optional.empty();
        }

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ComponentIndex(entries));
    }

    public static ComponentDefinition toStubComponentDefinition(Entry entry) {
        ModifiableComponentDefinition componentDefinition = component(entry.name())
            .version(entry.version());

        if (entry.title() != null) {
            componentDefinition.title(entry.title());
        }

        if (entry.description() != null) {
            componentDefinition.description(entry.description());
        }

        if (entry.icon() != null) {
            componentDefinition.icon(entry.icon());
        }

        List<CategorySummary> componentCategories = entry.componentCategories();

        if (!componentCategories.isEmpty()) {
            componentDefinition.categories(
                componentCategories.stream()
                    .map(category -> new ComponentCategory(category.name(), category.label()))
                    .toList());
        }

        List<String> tags = entry.tags();

        if (!tags.isEmpty()) {
            componentDefinition.tags(tags.toArray(String[]::new));
        }

        ConnectionSummary connectionSummary = entry.connection();

        if (connectionSummary != null) {
            componentDefinition.connection(
                connection()
                    .version(connectionSummary.version())
                    .authorizationRequired(connectionSummary.authorizationRequired()));
        }

        List<ItemSummary> actions = entry.actions();

        if (!actions.isEmpty()) {
            componentDefinition.actions(
                actions.stream()
                    .map(ComponentIndex::toStubActionDefinition)
                    .toList());
        }

        List<TriggerSummary> triggers = entry.triggers();

        if (!triggers.isEmpty()) {
            componentDefinition.triggers(
                triggers.stream()
                    .map(ComponentIndex::toStubTriggerDefinition)
                    .toList());
        }

        List<ClusterElementSummary> clusterElements = entry.clusterElements();

        if (!clusterElements.isEmpty()) {
            componentDefinition.clusterElements(
                clusterElements.stream()
                    .map(ComponentIndex::toStubClusterElementDefinition)
                    .toArray(ClusterElementDefinition<?>[]::new));
        }

        List<String> inputs = entry.inputs();

        if (!inputs.isEmpty()) {
            componentDefinition.inputs(
                inputs.stream()
                    .map(ComponentDsl::propertyGroup)
                    .toArray(ModifiablePropertyGroup[]::new));
        }

        UnifiedApiCategory unifiedApiCategory = entry.unifiedApiCategory();

        if (unifiedApiCategory != null) {
            componentDefinition.unifiedApi(unifiedApi(unifiedApiCategory));
        }

        return componentDefinition;
    }

    private static ModifiableActionDefinition toStubActionDefinition(ItemSummary itemSummary) {
        ModifiableActionDefinition actionDefinition = action(itemSummary.name());

        if (itemSummary.title() != null) {
            actionDefinition.title(itemSummary.title());
        }

        if (itemSummary.description() != null) {
            actionDefinition.description(itemSummary.description());
        }

        return actionDefinition;
    }

    private static ModifiableTriggerDefinition toStubTriggerDefinition(TriggerSummary triggerSummary) {
        ModifiableTriggerDefinition triggerDefinition = trigger(triggerSummary.name());

        if (triggerSummary.type() != null) {
            triggerDefinition.type(triggerSummary.type());
        }

        if (triggerSummary.title() != null) {
            triggerDefinition.title(triggerSummary.title());
        }

        if (triggerSummary.description() != null) {
            triggerDefinition.description(triggerSummary.description());
        }

        return triggerDefinition;
    }

    private static ModifiableClusterElementDefinition<?> toStubClusterElementDefinition(
        ClusterElementSummary clusterElementSummary) {

        ModifiableClusterElementDefinition<Object> clusterElementDefinition = clusterElement(
            clusterElementSummary.name());

        clusterElementDefinition.type(
            new ClusterElementType(
                clusterElementSummary.typeName(), clusterElementSummary.typeKey(), clusterElementSummary.typeLabel(),
                clusterElementSummary.typeMultipleElements(), clusterElementSummary.typeRequired()));

        if (clusterElementSummary.title() != null) {
            clusterElementDefinition.title(clusterElementSummary.title());
        }

        if (clusterElementSummary.description() != null) {
            clusterElementDefinition.description(clusterElementSummary.description());
        }

        return clusterElementDefinition;
    }

    @SuppressFBWarnings("EI")
    public record Entry(
        String name, int version, @Nullable String title, @Nullable String description, @Nullable String icon,
        List<CategorySummary> componentCategories, List<String> tags, @Nullable ConnectionSummary connection,
        List<ItemSummary> actions, List<TriggerSummary> triggers, List<ClusterElementSummary> clusterElements,
        List<String> inputs, @Nullable UnifiedApiCategory unifiedApiCategory, String providerClassName,
        ComponentHandlerLoaderKind loaderKind) {

        public Entry {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(providerClassName, "providerClassName");
            Objects.requireNonNull(loaderKind, "loaderKind");

            componentCategories = copyOrEmpty(componentCategories);
            tags = copyOrEmpty(tags);
            actions = copyOrEmpty(actions);
            triggers = copyOrEmpty(triggers);
            clusterElements = copyOrEmpty(clusterElements);
            inputs = copyOrEmpty(inputs);
        }

        private static <T> List<T> copyOrEmpty(@Nullable List<T> list) {
            return list == null ? List.of() : List.copyOf(list);
        }
    }

    public record CategorySummary(String name, @Nullable String label) {
    }

    public record ConnectionSummary(int version, boolean authorizationRequired) {
    }

    public record ItemSummary(String name, @Nullable String title, @Nullable String description) {
    }

    public record TriggerSummary(
        String name, @Nullable String title, @Nullable String description, @Nullable TriggerType type) {
    }

    public record ClusterElementSummary(
        String name, @Nullable String title, @Nullable String description, String typeName, String typeKey,
        String typeLabel, boolean typeMultipleElements, boolean typeRequired) {
    }
}
