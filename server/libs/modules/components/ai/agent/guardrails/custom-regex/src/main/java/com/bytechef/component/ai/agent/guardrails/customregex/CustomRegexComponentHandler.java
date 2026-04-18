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

package com.bytechef.component.ai.agent.guardrails.customregex;

import static com.bytechef.component.ai.agent.guardrails.customregex.CustomRegexComponentHandler.CUSTOM_REGEX;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.customregex.cluster.CustomRegex;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CUSTOM_REGEX + "_v1_ComponentHandler")
public class CustomRegexComponentHandler implements ComponentHandler {

    public static final String CUSTOM_REGEX = "customRegex";

    private final GuardrailComponentDefinition componentDefinition;

    public CustomRegexComponentHandler() {
        this.componentDefinition = new CustomRegexComponentDefinitionImpl(
            component(CUSTOM_REGEX)
                .title("Custom Regex")
                .description("User-defined regex guardrail — flags or masks matches of a custom pattern.")
                .icon("path:assets/custom-regex.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    CustomRegex.ofCheck(),
                    CustomRegex.ofSanitize()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class CustomRegexComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public CustomRegexComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return false;
        }
    }
}
