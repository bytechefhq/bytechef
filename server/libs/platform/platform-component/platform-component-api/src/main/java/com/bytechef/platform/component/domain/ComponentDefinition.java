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

package com.bytechef.platform.component.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.IconUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ComponentDefinition {

    private Map<String, List<String>> actionClusterElementTypes;
    private List<ActionDefinition> actions;
    private List<ComponentCategory> componentCategories;
    private boolean clusterElement;
    private List<ClusterElementDefinition> clusterElements;
    private List<ClusterElementType> clusterElementTypes;
    private boolean clusterRoot;
    private ConnectionDefinition connection;
    private boolean connectionRequired;
    private String description;
    private String icon;
    private String name;
    private Resources resources;
    private List<String> tags;
    private List<TriggerDefinition> triggers;
    private String title;
    private UnifiedApiCategory unifiedApiCategory;
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

        this.clusterElement = !OptionalUtils.orElse(componentDefinition.getClusterElements(), List.of())
            .isEmpty();
        this.clusterElements = getClusterElements(componentDefinition);

        if (componentDefinition instanceof ClusterRootComponentDefinition clusterRootComponentDefinition) {
            this.actionClusterElementTypes = clusterRootComponentDefinition.getActionClusterElementTypes();
            this.clusterElementTypes = clusterRootComponentDefinition.getClusterElementType();
        } else {
            this.clusterElementTypes = List.of();
        }

        this.clusterRoot = !clusterElementTypes.isEmpty();
        this.componentCategories = OptionalUtils.orElse(componentDefinition.getComponentCategories(), List.of());
        this.connection = getConnection(componentDefinition);
        this.connectionRequired = componentDefinition.getConnection()
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
        this.unifiedApiCategory = OptionalUtils.mapOrElse(
            componentDefinition.getUnifiedApi(), UnifiedApiDefinition::getCategory, null);
        this.version = componentDefinition.getVersion();
    }

    public boolean isClusterElement() {
        return clusterElement;
    }

    public boolean isClusterRoot() {
        return clusterRoot;
    }

    public boolean isConnectionRequired() {
        return connectionRequired;
    }

    public Map<String, List<String>> getActionClusterElementTypes() {
        return actionClusterElementTypes;
    }

    public List<ActionDefinition> getActions() {
        return actions;
    }

    public int getActionsCount() {
        return actions.size();
    }

    public List<ClusterElementDefinition> getClusterElements() {
        return clusterElements;
    }

    public Map<String, Integer> getClusterElementsCount() {
        return clusterElements.stream()
            .collect(Collectors.groupingBy(
                element -> {
                    ClusterElementType type = element.getType();

                    return type.name();
                },
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    public List<ComponentCategory> getComponentCategories() {
        return componentCategories;
    }

    public List<ClusterElementType> getClusterElementTypes() {
        return clusterElementTypes;
    }

    @Nullable
    public ConnectionDefinition getConnection() {
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

    public List<TriggerDefinition> getTriggers() {
        return triggers;
    }

    public int getTriggersCount() {
        return triggers.size();
    }

    @Nullable
    public UnifiedApiCategory getUnifiedApiCategory() {
        return unifiedApiCategory;
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

        return Objects.equals(actions, that.actions) && clusterElement == that.clusterElement &&
            Objects.equals(clusterElements, that.clusterElements) && clusterRoot == that.clusterRoot &&
            Objects.equals(componentCategories, that.componentCategories) &&
            Objects.equals(connection, that.connection) && connectionRequired == that.connectionRequired &&
            Objects.equals(description, that.description) && Objects.equals(icon, that.icon) &&
            Objects.equals(name, that.name) && Objects.equals(resources, that.resources) &&
            Objects.equals(tags, that.tags) && Objects.equals(triggers, that.triggers) &&
            Objects.equals(unifiedApiCategory, that.unifiedApiCategory) && Objects.equals(title, that.title) &&
            version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            actions, clusterElement, clusterElements, clusterRoot, componentCategories, connection, connectionRequired,
            description, icon, name, resources, tags, triggers, title, unifiedApiCategory, version);
    }

    @Override
    public String toString() {
        return "ComponentDefinition{" +
            "name='" + name + '\'' +
            ", version=" + version +
            ", title='" + title + '\'' +
            ", connection=" + connection +
            ", connectionRequired=" + connectionRequired +
            ", connectionRequired=" + connectionRequired +
            ", unifiedApiCategory=" + unifiedApiCategory +
            ", description='" + description + '\'' +
            ", icon='" + icon + '\'' +
            ", actions=" + actions +
            ", triggers=" + triggers +
            ", clusterElements=" + clusterElements +
            ", categories='" + componentCategories + '\'' +
            ", resources=" + resources +
            ", tags=" + tags +
            '}';
    }

    private static List<ActionDefinition> getActions(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return OptionalUtils.mapOrElse(
            componentDefinition.getActions(),
            actionDefinitions -> CollectionUtils.map(actionDefinitions, actionDefinition -> new ActionDefinition(
                actionDefinition, componentDefinition.getName(), componentDefinition.getVersion())),
            Collections.emptyList());
    }

    private static List<ClusterElementDefinition> getClusterElements(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return OptionalUtils.mapOrElse(
            componentDefinition.getClusterElements(),
            actionDefinitions -> CollectionUtils.map(actionDefinitions,
                clusterElementDefinition -> new ClusterElementDefinition(
                    clusterElementDefinition, componentDefinition.getName(), componentDefinition.getVersion(),
                    OptionalUtils.orElse(componentDefinition.getIcon(), null))),
            Collections.emptyList());
    }

    private static ConnectionDefinition getConnection(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        Optional<String> descriptionOptional = componentDefinition.getDescription();
        Optional<String> titleOptional = componentDefinition.getTitle();

        return OptionalUtils.mapOrElse(
            componentDefinition.getConnection(),
            connectionDefinition -> new ConnectionDefinition(
                connectionDefinition, componentDefinition.getName(),
                titleOptional.orElse(componentDefinition.getName()), descriptionOptional.orElse(null)),
            null);
    }

    private static String getTitle(String componentName, String componentTitle) {
        return componentTitle == null ? componentName : componentTitle;
    }

    private static List<TriggerDefinition> getTriggers(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return OptionalUtils.mapOrElse(
            componentDefinition.getTriggers(),
            triggerDefinitions -> CollectionUtils.map(triggerDefinitions,
                triggerDefinition -> new TriggerDefinition(
                    triggerDefinition, componentDefinition.getName(), componentDefinition.getVersion())),
            Collections.emptyList());
    }
}
