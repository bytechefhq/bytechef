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
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public record ComponentIndex(List<Entry> entries) {

    public static final String RESOURCE_PATH = "META-INF/bytechef/component-index.json";

    private static final Logger log = LoggerFactory.getLogger(ComponentIndex.class);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    public static Optional<ComponentIndex> load(ClassLoader classLoader) {
        List<Entry> entries = new ArrayList<>();

        Set<String> seenComponents = new HashSet<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(RESOURCE_PATH);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream inputStream = url.openStream()) {
                    ComponentIndex componentIndex = JSON_MAPPER.readValue(inputStream, ComponentIndex.class);

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

        if (entry.componentCategories() != null && !entry.componentCategories()
            .isEmpty()) {

            componentDefinition.categories(
                entry.componentCategories()
                    .stream()
                    .map(category -> new ComponentCategory(category.name(), category.label()))
                    .toList());
        }

        if (entry.tags() != null && !entry.tags()
            .isEmpty()) {

            componentDefinition.tags(entry.tags()
                .toArray(String[]::new));
        }

        if (entry.connection() != null) {
            componentDefinition.connection(
                connection()
                    .version(entry.connection()
                        .version())
                    .authorizationRequired(entry.connection()
                        .authorizationRequired()));
        }

        if (entry.actions() != null && !entry.actions()
            .isEmpty()) {

            componentDefinition.actions(
                entry.actions()
                    .stream()
                    .map(ComponentIndex::toStubActionDefinition)
                    .toList());
        }

        if (entry.triggers() != null && !entry.triggers()
            .isEmpty()) {

            componentDefinition.triggers(
                entry.triggers()
                    .stream()
                    .map(ComponentIndex::toStubTriggerDefinition)
                    .toList());
        }

        if (entry.clusterElements() != null && !entry.clusterElements()
            .isEmpty()) {

            componentDefinition.clusterElements(
                entry.clusterElements()
                    .stream()
                    .map(ComponentIndex::toStubClusterElementDefinition)
                    .toArray(ClusterElementDefinition<?>[]::new));
        }

        if (entry.inputs() != null && !entry.inputs()
            .isEmpty()) {

            componentDefinition.inputs(
                entry.inputs()
                    .stream()
                    .map(ComponentDsl::propertyGroup)
                    .toArray(ModifiablePropertyGroup[]::new));
        }

        List<ClusterElementTypeSummary> clusterElementTypes = entry.clusterElementTypes();

        if (clusterElementTypes == null || clusterElementTypes.isEmpty()) {
            return componentDefinition;
        }

        return new StubClusterRootComponentDefinition(
            componentDefinition,
            clusterElementTypes
                .stream()
                .map(
                    clusterElementType -> new ClusterElementType(
                        clusterElementType.name(), clusterElementType.key(), clusterElementType.label(),
                        clusterElementType.multipleElements(), clusterElementType.required()))
                .toList());
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
            triggerDefinition.type(TriggerType.valueOf(triggerSummary.type()));
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

    public record Entry(
        String name, int version, @Nullable String title, @Nullable String description, @Nullable String icon,
        @Nullable List<CategorySummary> componentCategories, @Nullable List<String> tags,
        @Nullable ConnectionSummary connection, @Nullable List<ItemSummary> actions,
        @Nullable List<TriggerSummary> triggers, @Nullable List<ClusterElementSummary> clusterElements,
        @Nullable List<ClusterElementTypeSummary> clusterElementTypes, @Nullable List<String> inputs,
        String providerClassName, String loaderKind) {
    }

    private static final class StubClusterRootComponentDefinition extends AbstractComponentDefinitionWrapper
        implements ClusterRootComponentDefinition {

        private final List<ClusterElementType> clusterElementTypes;

        private StubClusterRootComponentDefinition(
            ModifiableComponentDefinition componentDefinition, List<ClusterElementType> clusterElementTypes) {

            super(componentDefinition);

            this.clusterElementTypes = List.copyOf(clusterElementTypes);
        }

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return clusterElementTypes;
        }
    }

    public record CategorySummary(String name, @Nullable String label) {
    }

    public record ConnectionSummary(int version, boolean authorizationRequired) {
    }

    public record ItemSummary(String name, @Nullable String title, @Nullable String description) {
    }

    public record TriggerSummary(
        String name, @Nullable String title, @Nullable String description, @Nullable String type) {
    }

    public record ClusterElementSummary(
        String name, @Nullable String title, @Nullable String description, String typeName, String typeKey,
        String typeLabel, boolean typeMultipleElements, boolean typeRequired) {
    }

    public record ClusterElementTypeSummary(
        String name, String key, String label, boolean multipleElements, boolean required) {
    }
}
