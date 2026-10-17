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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.util.CustomActionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionWrapper extends AbstractComponentDefinitionWrapper implements ComponentDefinition {

    private final List<ActionDefinition> actions;
    private final List<ClusterElementDefinition<?>> clusterElements;

    public ComponentDefinitionWrapper(
        ComponentDefinition componentDefinition, List<ActionDefinition> actionDefinitions) {

        this(componentDefinition, actionDefinitions, componentDefinition.getClusterElements());
    }

    @SuppressFBWarnings("EI")
    public ComponentDefinitionWrapper(
        ComponentDefinition componentDefinition, List<ActionDefinition> actionDefinitions,
        List<ClusterElementDefinition<?>> clusterElementDefinitions) {

        super(componentDefinition);

        // Custom Actions support

        boolean exists = componentDefinition.getCustomAction();

        if (exists) {
            actionDefinitions = new ArrayList<>(actionDefinitions);

            actionDefinitions.add(CustomActionUtils.getCustomActionDefinition(componentDefinition));
        }

        this.actions = actionDefinitions;
        this.clusterElements = clusterElementDefinitions;
    }

    @Override
    public List<ActionDefinition> getActions() {
        return actions == null ? List.of() : new ArrayList<>(actions);
    }

    @Override
    public List<ClusterElementDefinition<?>> getClusterElements() {
        return clusterElements == null ? List.of() : new ArrayList<>(clusterElements);
    }

    @Override
    public String toString() {
        return "ComponentDefinitionWrapper{" +
            "name='" + name + '\'' +
            ", version=" + version +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", connection=" + connection +
            ", categories='" + componentCategories + '\'' +
            ", clusterElements=" + clusterElements +
            ", customAction=" + customAction +
            ", customActionHelp=" + customActionHelp +
            ", actions=" + actions +
            ", triggers=" + triggers +
            ", resources=" + resources +
            ", tags=" + tags +
            ", metadata=" + metadata +
            ", icon='" + icon + '\'' +
            "} ";
    }
}
