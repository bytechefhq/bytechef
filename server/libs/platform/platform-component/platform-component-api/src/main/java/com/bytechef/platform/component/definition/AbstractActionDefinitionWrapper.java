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

package com.bytechef.platform.component.definition;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionWorkflowNodeDescriptionFunction;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Output;
import com.bytechef.component.definition.Property;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractActionDefinitionWrapper implements ActionDefinition {

    protected final Boolean batch;
    protected final Boolean deprecated;
    protected final String description;
    protected final ActionWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction;
    protected final Help help;
    protected final Map<String, Object> metadata;
    protected final String name;
    protected final Output output;
    protected final OutputFunction outputSchemaFunction;
    protected final boolean outputSchemaDefaultFunction;
    protected final PerformFunction performFunction;
    protected final List<? extends Property> properties;
    protected final String title;

    public AbstractActionDefinitionWrapper(ActionDefinition actionDefinition) {
        this.batch = OptionalUtils.orElse(actionDefinition.getBatch(), null);
        this.deprecated = OptionalUtils.orElse(actionDefinition.getDeprecated(), null);
        this.description = OptionalUtils.orElse(actionDefinition.getDescription(), null);
        this.help = OptionalUtils.orElse(actionDefinition.getHelp(), null);
        this.metadata = OptionalUtils.orElse(actionDefinition.getMetadata(), null);
        this.name = actionDefinition.getName();
        this.output = OptionalUtils.orElse(actionDefinition.getOutput(), null);
        this.outputSchemaFunction = OptionalUtils.orElse(actionDefinition.getOutputFunction(), null);
        this.outputSchemaDefaultFunction = actionDefinition.isDefaultOutputFunction();
        this.performFunction = OptionalUtils.orElse(actionDefinition.getPerform(), null);
        this.properties = OptionalUtils.orElse(actionDefinition.getProperties(), null);
        this.title = OptionalUtils.orElse(actionDefinition.getTitle(), null);
        this.workflowNodeDescriptionFunction =
            OptionalUtils.orElse(actionDefinition.getWorkflowNodeDescriptionFunction(), null);
    }

    @Override
    public Optional<Boolean> getBatch() {
        return Optional.ofNullable(batch);
    }

    @Override
    public Optional<Boolean> getDeprecated() {
        return Optional.ofNullable(deprecated);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<Help> getHelp() {
        return Optional.ofNullable(help);
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
    public Optional<Output> getOutput() {
        return Optional.ofNullable(output);
    }

    @Override
    public Optional<OutputFunction> getOutputFunction() {
        return Optional.ofNullable(outputSchemaFunction);
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.ofNullable(performFunction);
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
    public Optional<ActionWorkflowNodeDescriptionFunction> getWorkflowNodeDescriptionFunction() {
        return Optional.ofNullable(workflowNodeDescriptionFunction);
    }

    @Override
    public boolean isDefaultOutputFunction() {
        return outputSchemaDefaultFunction;
    }
}
