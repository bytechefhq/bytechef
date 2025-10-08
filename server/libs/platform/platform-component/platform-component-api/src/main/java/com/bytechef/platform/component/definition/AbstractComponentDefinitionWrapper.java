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

package com.bytechef.platform.component.definition;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Resources;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentDefinitionWrapper implements ComponentDefinition {

    protected final List<ActionDefinition> actions;
    protected final List<ComponentCategory> componentCategories;
    protected final ConnectionDefinition connection;
    protected final Boolean customAction;
    protected final Help customActionHelp;
    protected final List<ClusterElementDefinition<?>> clusterElements;
    protected final String description;
    protected final String icon;
    protected final List<String> tags;
    protected final Map<String, Object> metadata;
    protected final String name;
    protected final Resources resources;
    protected final String title;
    protected final List<TriggerDefinition> triggers;
    protected final UnifiedApiDefinition unifiedApi;
    protected final int version;

    public AbstractComponentDefinitionWrapper(ComponentDefinition componentDefinition) {
        this.actions = OptionalUtils.orElse(componentDefinition.getActions(), List.of());
        this.componentCategories = OptionalUtils.orElse(componentDefinition.getComponentCategories(), List.of());
        this.connection = OptionalUtils.orElse(componentDefinition.getConnection(), null);
        this.customAction = OptionalUtils.orElse(componentDefinition.getCustomAction(), null);
        this.customActionHelp = OptionalUtils.orElse(componentDefinition.getCustomActionHelp(), null);
        this.clusterElements = OptionalUtils.orElse(componentDefinition.getClusterElements(), null);
        this.description = OptionalUtils.orElse(componentDefinition.getDescription(), null);
        this.icon = OptionalUtils.orElse(componentDefinition.getIcon(), null);
        this.tags = OptionalUtils.orElse(componentDefinition.getTags(), null);
        this.metadata = OptionalUtils.orElse(componentDefinition.getMetadata(), null);
        this.name = componentDefinition.getName();
        this.resources = OptionalUtils.orElse(componentDefinition.getResources(), null);
        this.title = OptionalUtils.orElse(componentDefinition.getTitle(), null);
        this.triggers = OptionalUtils.orElse(componentDefinition.getTriggers(), null);
        this.unifiedApi = OptionalUtils.orElse(componentDefinition.getUnifiedApi(), null);
        this.version = componentDefinition.getVersion();
    }

    @Override
    public Optional<List<ActionDefinition>> getActions() {
        return Optional.ofNullable(actions == null ? null : new ArrayList<>(actions));
    }

    @Override
    public Optional<List<ComponentCategory>> getComponentCategories() {
        return Optional.ofNullable(componentCategories == null ? null : new ArrayList<>(componentCategories));
    }

    @Override
    public Optional<List<ClusterElementDefinition<?>>> getClusterElements() {
        return Optional.ofNullable(clusterElements);
    }

    @Override
    public Optional<ConnectionDefinition> getConnection() {
        return Optional.ofNullable(connection);
    }

    @Override
    public Optional<Boolean> getCustomAction() {
        return Optional.ofNullable(customAction);
    }

    @Override
    public Optional<Help> getCustomActionHelp() {
        return Optional.ofNullable(customActionHelp);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<String> getIcon() {
        return Optional.ofNullable(icon);
    }

    @Override
    public Optional<Map<String, Object>> getMetadata() {
        return Optional.ofNullable(metadata == null ? null : new HashMap<>(metadata));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressFBWarnings("EI")
    public Optional<Resources> getResources() {
        return Optional.ofNullable(resources);
    }

    @Override
    public Optional<List<String>> getTags() {
        return Optional.ofNullable(tags == null ? null : Collections.unmodifiableList(tags));
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @Override
    public Optional<List<TriggerDefinition>> getTriggers() {
        return Optional.ofNullable(triggers == null ? null : new ArrayList<>(triggers));
    }

    @Override
    public Optional<UnifiedApiDefinition> getUnifiedApi() {
        return Optional.ofNullable(unifiedApi);
    }

    @Override
    public int getVersion() {
        return version;
    }
}
