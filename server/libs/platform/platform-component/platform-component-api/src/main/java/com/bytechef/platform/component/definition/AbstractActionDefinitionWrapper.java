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
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractActionDefinitionWrapper implements ActionDefinition {

    protected final boolean batch;
    protected final BeforeResumeFunction beforeResumeFunction;
    protected final BeforeSuspendConsumer beforeSuspendConsumer;
    protected final BeforeTimeoutResumeFunction beforeTimeoutResumeFunction;
    protected final boolean deprecated;
    protected final String description;
    protected final ProcessErrorResponseFunction processErrorResponseFunction;
    protected final Help help;
    protected final Map<String, Object> metadata;
    protected final String name;
    protected final OutputDefinition outputSchemaFunction;
    protected final BasePerformFunction performFunction;
    protected final List<? extends Property> properties;
    protected final BaseResumePerformFunction resumePerformFunction;
    protected final String title;
    protected final WorkflowNodeDescriptionFunction workflowNodeDescriptionFunction;

    public AbstractActionDefinitionWrapper(ActionDefinition actionDefinition) {
        this.batch = actionDefinition.getBatch();
        this.beforeResumeFunction = actionDefinition.getBeforeResume()
            .orElse(null);
        this.beforeSuspendConsumer = actionDefinition.getBeforeSuspend()
            .orElse(null);
        this.beforeTimeoutResumeFunction = actionDefinition.getBeforeTimeoutResume()
            .orElse(null);
        this.deprecated = actionDefinition.getDeprecated();
        this.description = OptionalUtils.orElse(actionDefinition.getDescription(), null);
        this.processErrorResponseFunction = OptionalUtils.orElse(actionDefinition.getProcessErrorResponse(), null);
        this.help = OptionalUtils.orElse(actionDefinition.getHelp(), null);
        this.metadata = actionDefinition.getMetadata();
        this.name = actionDefinition.getName();
        this.outputSchemaFunction = OptionalUtils.orElse(actionDefinition.getOutputDefinition(), null);
        this.performFunction = OptionalUtils.orElse(actionDefinition.getPerform(), null);
        this.properties = actionDefinition.getProperties();
        this.resumePerformFunction = actionDefinition.getResumePerform()
            .orElse(null);
        this.title = OptionalUtils.orElse(actionDefinition.getTitle(), null);
        this.workflowNodeDescriptionFunction =
            OptionalUtils.orElse(actionDefinition.getWorkflowNodeDescription(), null);
    }

    @Override
    public boolean getBatch() {
        return batch;
    }

    @Override
    public Optional<BeforeSuspendConsumer> getBeforeSuspend() {
        return Optional.ofNullable(beforeSuspendConsumer);
    }

    @Override
    public Optional<BeforeResumeFunction> getBeforeResume() {
        return Optional.ofNullable(beforeResumeFunction);
    }

    @Override
    public Optional<BeforeTimeoutResumeFunction> getBeforeTimeoutResume() {
        return Optional.ofNullable(beforeTimeoutResumeFunction);
    }

    @Override
    public boolean getDeprecated() {
        return deprecated;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<ProcessErrorResponseFunction> getProcessErrorResponse() {
        return Optional.ofNullable(processErrorResponseFunction);
    }

    @Override
    public Optional<Help> getHelp() {
        return Optional.ofNullable(help);
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? Map.of() : new HashMap<>(metadata);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<OutputDefinition> getOutputDefinition() {
        return Optional.ofNullable(outputSchemaFunction);
    }

    @Override
    public Optional<? extends BasePerformFunction> getPerform() {
        return Optional.ofNullable(performFunction);
    }

    @Override
    public List<? extends Property> getProperties() {
        return properties == null ? List.of() : properties;
    }

    @Override
    public Optional<? extends BaseResumePerformFunction> getResumePerform() {
        return Optional.ofNullable(resumePerformFunction);
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @Override
    public Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription() {
        return Optional.ofNullable(workflowNodeDescriptionFunction);
    }
}
