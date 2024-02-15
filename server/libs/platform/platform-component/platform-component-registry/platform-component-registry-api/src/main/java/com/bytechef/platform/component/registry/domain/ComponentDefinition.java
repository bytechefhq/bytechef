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
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ComponentDefinition {

    private List<ActionDefinitionBasic> actions;
    private String category;
    private ConnectionDefinitionBasic connection;
    private String description;
    private String icon;
    private String name;
    private Resources resources;
    private List<String> tags;
    private List<TriggerDefinitionBasic> triggers;
    private String title;
    private int version;
    private List<String> workflowConnectionKeys;

    private ComponentDefinition() {
    }

    public ComponentDefinition(String name) {
        this.actions = List.of();
        this.name = name;
        this.tags = List.of();
        this.triggers = List.of();
        this.workflowConnectionKeys = List.of();
    }

    public ComponentDefinition(com.bytechef.component.definition.ComponentDefinition componentDefinition) {
        this.actions = getActions(componentDefinition);
        this.category = OptionalUtils.orElse(componentDefinition.getCategory(), null);
        this.connection =
            OptionalUtils.mapOrElse(componentDefinition.getConnection(), ConnectionDefinitionBasic::new, null);
        this.description = OptionalUtils.orElse(componentDefinition.getDescription(), null);
        this.icon = OptionalUtils.mapOrElse(componentDefinition.getIcon(), IconUtils::readIcon, null);
        this.name = componentDefinition.getName();
        this.resources = OptionalUtils.mapOrElse(componentDefinition.getResources(), Resources::new, null);
        this.tags = OptionalUtils.orElse(componentDefinition.getTags(), Collections.emptyList());
        this.triggers = OptionalUtils.mapOrElse(
            componentDefinition.getTriggers(),
            triggerDefinitions -> CollectionUtils.map(triggerDefinitions, TriggerDefinitionBasic::new),
            Collections.emptyList());
        this.title = getTitle(
            componentDefinition.getName(), OptionalUtils.orElse(componentDefinition.getTitle(), null));
        this.version = componentDefinition.getVersion();
        this.workflowConnectionKeys = componentDefinition.getWorkflowConnectionKeys();
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

    public List<String> getWorkflowConnectionKeys() {
        return workflowConnectionKeys;
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
            Objects.equals(connection, that.connection) && Objects.equals(description, that.description) &&
            Objects.equals(icon, that.icon) && Objects.equals(name, that.name) &&
            Objects.equals(resources, that.resources) && Objects.equals(tags, that.tags) &&
            Objects.equals(triggers, that.triggers) && Objects.equals(title, that.title) &&
            version == that.version && Objects.equals(workflowConnectionKeys, that.workflowConnectionKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            actions, category, connection, description, icon, name, resources, tags, triggers, title, version,
            workflowConnectionKeys);
    }

    @Override
    public String toString() {
        return "ComponentDefinition{" +
            "actions=" + actions +
            ", category='" + category + '\'' +
            ", connection=" + connection +
            ", description='" + description + '\'' +
            ", icon='" + icon + '\'' +
            ", name='" + name + '\'' +
            ", resources=" + resources +
            ", tags=" + tags +
            ", triggers=" + triggers +
            ", workflowConnectionKeys=" + workflowConnectionKeys +
            ", title='" + title + '\'' +
            ", version=" + version +
            '}';
    }

    private static List<ActionDefinitionBasic> getActions(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return OptionalUtils.mapOrElse(
            componentDefinition.getActions(),
            actionDefinitions -> CollectionUtils.map(actionDefinitions, ActionDefinitionBasic::new),
            Collections.emptyList());
    }

    private static String getTitle(String componentName, String componentTitle) {
        return componentTitle == null ? componentName : componentTitle;
    }
}
