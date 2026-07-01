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

package com.bytechef.platform.configuration.domain;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * Gives domain meaning to the generic per-input {@link Workflow.Input#extensions()} map, the same way
 * {@link WorkflowTrigger} gives meaning to the generic extensions of {@link Workflow}.
 *
 * @author Ivica Cardic
 */
public class WorkflowInput {

    private final Workflow.Input input;

    public WorkflowInput(Workflow.Input input) {
        Assert.notNull(input, "'input' must not be null");

        this.input = input;
    }

    public static List<WorkflowInput> of(Workflow workflow) {
        return CollectionUtils.map(workflow.getInputs(), WorkflowInput::new);
    }

    public String getName() {
        return input.name();
    }

    public String getLabel() {
        return input.label();
    }

    public String getType() {
        return input.type();
    }

    public boolean isRequired() {
        return input.required();
    }

    public <T> T getExtension(String name, Class<T> elementType, T defaultValue) {
        return input.getExtension(name, elementType, defaultValue);
    }

    public Map<String, ?> getExtensions() {
        return input.extensions();
    }

    /**
     * Resolves the component-input-group reference carried in the input extensions, or {@code null} when the input does
     * not reference a component.
     */
    public ComponentInputReference getComponentInputReference() {
        String componentName = getExtension(WorkflowExtConstants.COMPONENT_NAME, String.class, null);

        if (componentName == null) {
            return null;
        }

        return new ComponentInputReference(
            componentName,
            getExtension(WorkflowExtConstants.COMPONENT_VERSION, Integer.class, null),
            getExtension(WorkflowExtConstants.GROUP_NAME, String.class, null));
    }

    public String getObjectName() {
        return getExtension(WorkflowExtConstants.OBJECT_NAME, String.class, null);
    }

    public boolean isInternalOnly() {
        return getExtension(WorkflowExtConstants.INTERNAL_ONLY, Boolean.class, false);
    }

    public record ComponentInputReference(String componentName, Integer componentVersion, String groupName)
        implements Serializable {

        public ComponentInputReference {
            Assert.notNull(componentName, "componentName is required");
            Assert.notNull(componentVersion, "componentVersion is required");
            Assert.notNull(groupName, "groupName is required");
        }
    }
}
