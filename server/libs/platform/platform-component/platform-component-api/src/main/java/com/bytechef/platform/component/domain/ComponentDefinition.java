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
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class ComponentDefinition {

    private Map<String, List<String>> actionClusterElementTypes;
    private List<ActionDefinition> actions;
    private List<AgentChannelDefinition> agentChannels;
    private List<ComponentCategory> componentCategories;
    private boolean clusterElement;
    private Map<String, List<String>> clusterElementClusterElementTypes;
    private List<ClusterElementDefinition> clusterElements;
    private List<ClusterElementType> clusterElementTypes;
    private boolean clusterRoot;
    private ConnectionDefinition connection;
    private boolean connectionRequired;
    private String description;
    private String icon;
    private String name;
    private List<PropertyGroup> inputs;
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
        this.agentChannels = List.of();
        this.name = name;
        this.inputs = List.of();
        this.tags = List.of();
        this.triggers = List.of();
    }

    public ComponentDefinition(
        ComponentDefinition componentDefinition, List<ActionDefinition> actions, List<TriggerDefinition> triggers) {

        this.actionClusterElementTypes = componentDefinition.actionClusterElementTypes;
        this.actions = actions;
        this.agentChannels = componentDefinition.agentChannels;
        this.componentCategories = componentDefinition.componentCategories;
        this.clusterElement = componentDefinition.clusterElement;
        this.clusterElementClusterElementTypes = componentDefinition.clusterElementClusterElementTypes;
        this.clusterElements = componentDefinition.clusterElements;
        this.clusterElementTypes = componentDefinition.clusterElementTypes;
        this.clusterRoot = componentDefinition.clusterRoot;
        this.connection = componentDefinition.connection;
        this.connectionRequired = componentDefinition.connectionRequired;
        this.description = componentDefinition.description;
        this.icon = componentDefinition.icon;
        this.name = componentDefinition.name;
        this.inputs = componentDefinition.inputs;
        this.resources = componentDefinition.resources;
        this.tags = componentDefinition.tags;
        this.triggers = triggers;
        this.title = componentDefinition.title;
        this.unifiedApiCategory = componentDefinition.unifiedApiCategory;
        this.version = componentDefinition.version;
    }

    public ComponentDefinition(com.bytechef.component.definition.ComponentDefinition componentDefinition) {
        this.actions = getActions(componentDefinition);
        this.agentChannels = componentDefinition.getAgentChannels()
            .stream()
            .map(
                agentChannelDefinition -> new AgentChannelDefinition(
                    agentChannelDefinition, componentDefinition.getName(), componentDefinition.getVersion()))
            .toList();
        this.inputs = getInputs(componentDefinition);

        this.clusterElement = !componentDefinition.getClusterElements()
            .isEmpty();
        this.clusterElements = getClusterElements(componentDefinition);

        if (componentDefinition instanceof ClusterRootComponentDefinition clusterRootComponentDefinition) {
            this.actionClusterElementTypes = clusterRootComponentDefinition.getActionClusterElementTypes();
            this.clusterElementClusterElementTypes = clusterRootComponentDefinition
                .getClusterElementClusterElementTypes();
            this.clusterElementTypes = clusterRootComponentDefinition.getClusterElementTypes();
        } else {
            this.clusterElementTypes = List.of();
        }

        this.clusterRoot = !clusterElementTypes.isEmpty();
        this.componentCategories = componentDefinition.getComponentCategories();
        this.connection = getConnection(componentDefinition);
        this.connectionRequired = componentDefinition.getConnection()
            .map(connectionDefinition -> {
                List<? extends Property> properties = connectionDefinition.getProperties();
                boolean authorizationRequired = connectionDefinition.getAuthorizationRequired();

                return CollectionUtils.anyMatch(properties, Property::getRequired) || authorizationRequired;
            })
            .orElse(false);
        this.description = componentDefinition.getDescription()
            .orElse(null);
        this.icon = componentDefinition.getIcon()
            .map(IconUtils::readIcon)
            .orElse(null);
        this.name = Objects.requireNonNull(componentDefinition.getName(), "name is required");
        this.resources = componentDefinition.getResources()
            .map(Resources::new)
            .orElse(null);
        this.tags = componentDefinition.getTags();
        this.triggers = getTriggers(componentDefinition);
        this.title = getTitle(componentDefinition.getName(), componentDefinition.getTitle()
            .orElse(null));
        this.unifiedApiCategory = componentDefinition.getUnifiedApi()
            .map(UnifiedApiDefinition::getCategory)
            .orElse(null);
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

    public List<AgentChannelDefinition> getAgentChannels() {
        return agentChannels;
    }

    public Map<String, List<String>> getClusterElementClusterElementTypes() {
        return clusterElementClusterElementTypes;
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

    public List<PropertyGroup> getInputs() {
        return inputs;
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
        if (!(o instanceof ComponentDefinition that)) {
            return false;
        }

        return clusterElement == that.clusterElement && clusterRoot == that.clusterRoot &&
            connectionRequired == that.connectionRequired && version == that.version &&
            Objects.equals(actionClusterElementTypes, that.actionClusterElementTypes) &&
            Objects.equals(actions, that.actions) && Objects.equals(agentChannels, that.agentChannels) &&
            Objects.equals(componentCategories, that.componentCategories) &&
            Objects.equals(clusterElementClusterElementTypes, that.clusterElementClusterElementTypes) &&
            Objects.equals(clusterElements, that.clusterElements) &&
            Objects.equals(clusterElementTypes, that.clusterElementTypes) &&
            Objects.equals(connection, that.connection) && Objects.equals(description, that.description) &&
            Objects.equals(icon, that.icon) && Objects.equals(name, that.name) &&
            Objects.equals(inputs, that.inputs) &&
            Objects.equals(resources, that.resources) && Objects.equals(tags, that.tags) &&
            Objects.equals(triggers, that.triggers) && Objects.equals(title, that.title) &&
            unifiedApiCategory == that.unifiedApiCategory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            actionClusterElementTypes, actions, agentChannels, componentCategories, clusterElement,
            clusterElementClusterElementTypes, clusterElements, clusterElementTypes, clusterRoot, connection,
            connectionRequired, description, icon, name, inputs, resources, tags, triggers, title, unifiedApiCategory,
            version);
    }

    @Override
    public String toString() {
        return "ComponentDefinition{" +
            "name='" + name + '\'' +
            ", version=" + version +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", connection=" + connection +
            ", connectionRequired=" + connectionRequired +
            ", clusterRoot=" + clusterRoot +
            ", clusterElement=" + clusterElement +
            ", actions=" + actions +
            ", agentChannels=" + agentChannels +
            ", inputs=" + inputs +
            ", triggers=" + triggers +
            ", clusterElementTypes=" + clusterElementTypes +
            ", actionClusterElementTypes=" + actionClusterElementTypes +
            ", clusterElementClusterElementTypes=" + clusterElementClusterElementTypes +
            ", clusterElements=" + clusterElements +
            ", componentCategories=" + componentCategories +
            ", unifiedApiCategory=" + unifiedApiCategory +
            ", icon='" + icon + '\'' +
            ", resources=" + resources +
            ", tags=" + tags +
            '}';
    }

    private static List<ActionDefinition> getActions(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return CollectionUtils.map(componentDefinition.getActions(), actionDefinition -> {
            int componentVersion = componentDefinition.getVersion();

            return new ActionDefinition(actionDefinition, componentDefinition.getName(), componentVersion);
        });
    }

    private static List<ClusterElementDefinition> getClusterElements(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return CollectionUtils.map(
            componentDefinition.getClusterElements(),
            clusterElementDefinition -> {
                int componentVersion = componentDefinition.getVersion();
                String icon = componentDefinition.getIcon()
                    .orElse(null);

                return new ClusterElementDefinition(
                    clusterElementDefinition, componentDefinition.getName(), componentVersion, icon);
            });
    }

    private static List<PropertyGroup> getInputs(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return CollectionUtils.map(componentDefinition.getInputs(), PropertyGroup::new);
    }

    private static ConnectionDefinition getConnection(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        Optional<String> descriptionOptional = componentDefinition.getDescription();
        Optional<String> titleOptional = componentDefinition.getTitle();

        return componentDefinition.getConnection()
            .map(connectionDefinition -> new ConnectionDefinition(
                connectionDefinition, componentDefinition.getName(),
                titleOptional.orElse(componentDefinition.getName()), descriptionOptional.orElse(null)))
            .orElse(null);
    }

    private static String getTitle(String componentName, String componentTitle) {
        return componentTitle == null ? componentName : componentTitle;
    }

    private static List<TriggerDefinition> getTriggers(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return CollectionUtils.map(componentDefinition.getTriggers(),
            triggerDefinition -> {
                int componentVersion = componentDefinition.getVersion();

                return new TriggerDefinition(triggerDefinition, componentDefinition.getName(), componentVersion);
            });
    }
}
