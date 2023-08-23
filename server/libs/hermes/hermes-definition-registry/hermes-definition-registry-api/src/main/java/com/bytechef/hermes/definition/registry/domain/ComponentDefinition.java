
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.registry.component.util.CustomActionUtils;
import com.bytechef.commons.util.IconUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ComponentDefinition {

    private final List<ActionDefinitionBasic> actions;
    private final String category;
    private final ConnectionDefinitionBasic connection;
    private final String description;
    private final String icon;
    private final String name;
    private final Resources resources;
    private final List<String> tags;
    private final List<TriggerDefinitionBasic> triggers;
    private final String title;
    private final int version;

    public ComponentDefinition(String name) {
        this.actions = List.of();
        this.category = null;
        this.connection = null;
        this.description = null;
        this.icon = null;
        this.resources = null;
        this.name = name;
        this.tags = List.of();
        this.title = null;
        this.triggers = List.of();
        this.version = 0;
    }

    public ComponentDefinition(com.bytechef.hermes.component.definition.ComponentDefinition componentDefinition) {
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
    }

    public List<ActionDefinitionBasic> getActions() {
        return actions;
    }

    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }

    public Optional<ConnectionDefinitionBasic> getConnection() {
        return Optional.ofNullable(connection);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getIcon() {
        return Optional.ofNullable(icon);
    }

    public String getName() {
        return name;
    }

    public Optional<Resources> getResources() {
        return Optional.ofNullable(resources);
    }

    public List<String> getTags() {
        return tags;
    }

    public List<TriggerDefinitionBasic> getTriggers() {
        return triggers;
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComponentDefinition that))
            return false;
        return version == that.version && Objects.equals(actions, that.actions)
            && Objects.equals(category, that.category) && Objects.equals(connection, that.connection)
            && Objects.equals(description, that.description) && Objects.equals(icon, that.icon)
            && Objects.equals(name, that.name) && Objects.equals(resources, that.resources)
            && Objects.equals(tags, that.tags) && Objects.equals(triggers, that.triggers)
            && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, category, connection, description, icon, name, resources, tags, triggers, title,
            version);
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
            ", title='" + title + '\'' +
            ", version=" + version +
            '}';
    }

    private static List<ActionDefinitionBasic>
        getActions(com.bytechef.hermes.component.definition.ComponentDefinition componentDefinition) {
        List<ActionDefinitionBasic> actionDefinitionBasics = OptionalUtils.mapOrElse(
            componentDefinition.getActions(),
            actionDefinitions -> CollectionUtils.map(actionDefinitions, ActionDefinitionBasic::new),
            Collections.emptyList());

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actionDefinitionBasics = new ArrayList<>(actionDefinitionBasics);

            actionDefinitionBasics.add(
                new ActionDefinitionBasic(CustomActionUtils.getCustomActionDefinition(componentDefinition)));
        }

        return actionDefinitionBasics;
    }

    private static String getTitle(String componentName, String componentTitle) {
        return componentTitle == null ? componentName : componentTitle;
    }
}
