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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.PropertyGroup;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ProviderEntry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoaderKind;
import com.bytechef.platform.component.handler.loader.DefaultComponentHandlerLoader;
import com.bytechef.platform.component.index.ComponentIndex.CategorySummary;
import com.bytechef.platform.component.index.ComponentIndex.ClusterElementSummary;
import com.bytechef.platform.component.index.ComponentIndex.ConnectionSummary;
import com.bytechef.platform.component.index.ComponentIndex.Entry;
import com.bytechef.platform.component.index.ComponentIndex.ItemSummary;
import com.bytechef.platform.component.index.ComponentIndex.TriggerSummary;
import com.bytechef.platform.component.jdbc.handler.loader.JdbcComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class ComponentIndexGenerator {

    private static final List<ComponentHandlerLoader> COMPONENT_HANDLER_LOADERS = List.of(
        new DefaultComponentHandlerLoader(), new JdbcComponentHandlerLoader(), new OpenApiComponentHandlerLoader());

    private ComponentIndexGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ComponentIndexGenerator <output-file>");
        }

        Path outputPath = Path.of(args[0]);

        List<Entry> entries = new ArrayList<>();

        for (ComponentHandlerLoader componentHandlerLoader : COMPONENT_HANDLER_LOADERS) {
            for (ProviderEntry providerEntry : componentHandlerLoader.loadProviderEntries()) {
                ComponentDefinition componentDefinition = providerEntry.componentHandlerEntry()
                    .componentHandler()
                    .getDefinition();

                entries.add(
                    toEntry(
                        componentDefinition, providerEntry.providerClassName(), componentHandlerLoader.getKind()));
            }
        }

        entries.sort(Comparator.comparing(Entry::name)
            .thenComparing(Entry::version));

        Path parentPath = outputPath.getParent();

        if (parentPath != null) {
            Files.createDirectories(parentPath);
        }

        JsonMapper jsonMapper = JsonMapper.builder()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .build();

        Files.writeString(outputPath, jsonMapper.writeValueAsString(new ComponentIndex(entries)));
    }

    private static Entry toEntry(
        ComponentDefinition componentDefinition, String providerClassName, ComponentHandlerLoaderKind loaderKind) {

        return new Entry(
            componentDefinition.getName(),
            componentDefinition.getVersion(),
            componentDefinition.getTitle()
                .orElse(null),
            componentDefinition.getDescription()
                .orElse(null),
            componentDefinition.getIcon()
                .orElse(null),
            componentDefinition.getComponentCategories()
                .map(categories -> categories.stream()
                    .map(category -> new CategorySummary(category.name(), category.label()))
                    .toList())
                .orElse(null),
            componentDefinition.getTags()
                .orElse(null),
            componentDefinition.getConnection()
                .map(ComponentIndexGenerator::toConnectionSummary)
                .orElse(null),
            componentDefinition.getActions()
                .map(actions -> actions.stream()
                    .map(ComponentIndexGenerator::toItemSummary)
                    .toList())
                .orElse(null),
            componentDefinition.getTriggers()
                .map(triggers -> triggers.stream()
                    .map(ComponentIndexGenerator::toTriggerSummary)
                    .toList())
                .orElse(null),
            componentDefinition.getClusterElements()
                .map(clusterElements -> clusterElements.stream()
                    .map(ComponentIndexGenerator::toClusterElementSummary)
                    .toList())
                .orElse(null),
            componentDefinition.getInputs()
                .map(inputs -> inputs.stream()
                    .map(PropertyGroup::getName)
                    .toList())
                .orElse(null),
            componentDefinition.getUnifiedApi()
                .map(UnifiedApiDefinition::getCategory)
                .orElse(null),
            providerClassName,
            loaderKind);
    }

    private static ConnectionSummary toConnectionSummary(ConnectionDefinition connectionDefinition) {
        boolean anyPropertyRequired = connectionDefinition.getProperties()
            .orElse(List.of())
            .stream()
            .anyMatch(property -> Boolean.TRUE.equals(property.getRequired()));

        boolean authorizationRequired = connectionDefinition.getAuthorizationRequired()
            .orElse(true);

        return new ConnectionSummary(connectionDefinition.getVersion(), anyPropertyRequired || authorizationRequired);
    }

    private static ItemSummary toItemSummary(ActionDefinition actionDefinition) {
        return new ItemSummary(
            actionDefinition.getName(),
            actionDefinition.getTitle()
                .orElse(null),
            actionDefinition.getDescription()
                .orElse(null));
    }

    private static TriggerSummary toTriggerSummary(TriggerDefinition triggerDefinition) {
        return new TriggerSummary(
            triggerDefinition.getName(),
            triggerDefinition.getTitle()
                .orElse(null),
            triggerDefinition.getDescription()
                .orElse(null),
            triggerDefinition.getType());
    }

    private static ClusterElementSummary toClusterElementSummary(
        ClusterElementDefinition<?> clusterElementDefinition) {

        ClusterElementType clusterElementType = clusterElementDefinition.getType();

        return new ClusterElementSummary(
            clusterElementDefinition.getName(),
            clusterElementDefinition.getTitle()
                .orElse(null),
            clusterElementDefinition.getDescription()
                .orElse(null),
            clusterElementType.name(),
            clusterElementType.key(),
            clusterElementType.label(),
            clusterElementType.multipleElements(),
            clusterElementType.required());
    }
}
