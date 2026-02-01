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

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractClusterElementDefinitionWrapper<T> implements ClusterElementDefinition<T> {

    protected final String description;
    protected final T element;
    protected final Help help;
    protected final String name;
    protected final OutputDefinition outputDefinition;
    protected final ProcessErrorResponseFunction processErrorResponseFunction;
    protected final List<? extends Property> properties;
    protected final String title;
    protected final ClusterElementType type;
    protected final WorkflowNodeDescriptionFunction workflowNodeDescriptionFunction;

    @SuppressWarnings("unchecked")
    public AbstractClusterElementDefinitionWrapper(ClusterElementDefinition<?> clusterElementDefinition) {
        this.description = clusterElementDefinition.getDescription()
            .orElse(null);
        this.element = (T) clusterElementDefinition.getElement();
        this.help = clusterElementDefinition.getHelp()
            .orElse(null);
        this.name = clusterElementDefinition.getName();
        this.outputDefinition = clusterElementDefinition.getOutputDefinition()
            .orElse(null);
        this.processErrorResponseFunction = clusterElementDefinition.getProcessErrorResponse()
            .orElse(null);
        this.properties = clusterElementDefinition.getProperties()
            .orElse(null);
        this.title = clusterElementDefinition.getTitle()
            .orElse(null);
        this.type = clusterElementDefinition.getType();
        this.workflowNodeDescriptionFunction = clusterElementDefinition.getWorkflowNodeDescription()
            .orElse(null);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @JsonIgnore
    @Override
    public T getElement() {
        return element;
    }

    @Override
    public Optional<Help> getHelp() {
        return Optional.ofNullable(help);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<OutputDefinition> getOutputDefinition() {
        return Optional.ofNullable(outputDefinition);
    }

    @Override
    public Optional<ProcessErrorResponseFunction> getProcessErrorResponse() {
        return Optional.ofNullable(processErrorResponseFunction);
    }

    @Override
    public Optional<List<? extends Property>> getProperties() {
        return Optional.ofNullable(properties);
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @Override
    public ClusterElementType getType() {
        return type;
    }

    @Override
    public Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription() {
        return Optional.ofNullable(workflowNodeDescriptionFunction);
    }
}
