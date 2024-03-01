/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.IconUtils;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ComponentDefinition {

    private List<ActionDefinitionBasic> actions;
    private String category;
    private ConnectionDefinitionBasic connection;
    private boolean connectionRequired;
    private String description;
    private String icon;
    private String name;
    private Resources resources;
    private List<String> tags;
    private List<TriggerDefinitionBasic> triggers;
    private String title;
    private int version;

    private ComponentDefinition() {
    }

    public ComponentDefinition(String name) {
        this.actions = List.of();
        this.name = name;
        this.tags = List.of();
        this.triggers = List.of();
    }

    public ComponentDefinition(com.bytechef.component.definition.ComponentDefinition componentDefinition) {
        this.actions = getActions(componentDefinition);
        this.category = OptionalUtils.orElse(componentDefinition.getCategory(), null);
        this.connection = getConnection(componentDefinition);
        this.connectionRequired = componentDefinition
            .getConnection()
            .map(connectionDefinition -> CollectionUtils.anyMatch(
                OptionalUtils.orElse(connectionDefinition.getProperties(), List.of()),
                property -> OptionalUtils.orElse(property.getRequired(), false)) ||
                OptionalUtils.orElse(connectionDefinition.getAuthorizationRequired(), true))
            .orElse(false);
        this.description = OptionalUtils.orElse(componentDefinition.getDescription(), null);
        this.icon = OptionalUtils.mapOrElse(componentDefinition.getIcon(), IconUtils::readIcon, null);
        this.name = componentDefinition.getName();
        this.resources = OptionalUtils.mapOrElse(componentDefinition.getResources(), Resources::new, null);
        this.tags = OptionalUtils.orElse(componentDefinition.getTags(), Collections.emptyList());
        this.triggers = getTriggers(componentDefinition);
        this.title = getTitle(
            componentDefinition.getName(), OptionalUtils.orElse(componentDefinition.getTitle(), null));
        this.version = componentDefinition.getVersion();
    }

    public boolean isConnectionRequired() {
        return connectionRequired;
    }

    public List<ActionDefinitionBasic> getActions() {
        return actions;
    }

    public int getActionsCount() {
        return actions.size();
    }

    public String getCategory() {
        return category;
    }

    @Nullable
    public ConnectionDefinitionBasic getConnection() {
        return connection;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Resources getResources() {
        return resources;
    }

    public List<String> getTags() {
        return tags;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public List<TriggerDefinitionBasic> getTriggers() {
        return triggers;
    }

    public int getTriggersCount() {
        return triggers.size();
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ComponentDefinition that)) {
            return false;
        }

        return Objects.equals(actions, that.actions) && Objects.equals(category, that.category) &&
            Objects.equals(connection, that.connection) && connectionRequired == that.connectionRequired &&
            Objects.equals(description, that.description) && Objects.equals(icon, that.icon) &&
            Objects.equals(name, that.name) && Objects.equals(resources, that.resources) &&
            Objects.equals(tags, that.tags) && Objects.equals(triggers, that.triggers) &&
            Objects.equals(title, that.title) && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            actions, category, connection, connectionRequired, description, icon, name, resources, tags, triggers,
            title, version);
    }

    @Override
    public String toString() {
        return "ComponentDefinition{" +
            "actions=" + actions +
            ", category='" + category + '\'' +
            ", connection=" + connection +
            ", connectionRequired=" + connectionRequired +
            ", description='" + description + '\'' +
            ", icon='" + icon + '\'' +
            ", name='" + name + '\'' +
            ", resources=" + resources +
            ", tags=" + tags +
            ", triggers=" + triggers +
            ", title='" + title + '\'' +
            ", version=" + version +
            '}';
    }

    private static List<ActionDefinitionBasic> getActions(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return OptionalUtils.mapOrElse(
            componentDefinition.getActions(),
            actionDefinitions -> CollectionUtils.map(actionDefinitions, actionDefinition -> new ActionDefinitionBasic(
                actionDefinition, componentDefinition.getName(), componentDefinition.getVersion())),
            Collections.emptyList());
    }

    private static ConnectionDefinitionBasic getConnection(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        Optional<String> descriptionOptional = componentDefinition.getDescription();
        Optional<String> titleOptional = componentDefinition.getTitle();

        return OptionalUtils.mapOrElse(
            componentDefinition.getConnection(),
            connectionDefinition -> new ConnectionDefinitionBasic(
                connectionDefinition, componentDefinition.getName(),
                titleOptional.orElse(componentDefinition.getName()), descriptionOptional.orElse(null)),
            null);
    }

    private static String getTitle(String componentName, String componentTitle) {
        return componentTitle == null ? componentName : componentTitle;
    }

    private static List<TriggerDefinitionBasic> getTriggers(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return OptionalUtils.mapOrElse(
            componentDefinition.getTriggers(),
            triggerDefinitions -> CollectionUtils.map(triggerDefinitions,
                triggerDefinition -> new TriggerDefinitionBasic(
                    triggerDefinition, componentDefinition.getName(), componentDefinition.getVersion())),
            Collections.emptyList());
    }
}
