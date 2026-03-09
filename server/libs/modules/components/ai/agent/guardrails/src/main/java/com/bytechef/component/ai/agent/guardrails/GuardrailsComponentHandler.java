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

package com.bytechef.component.ai.agent.guardrails;

import static com.bytechef.component.ai.agent.guardrails.GuardrailsComponentHandler.GUARDRAILS_COMPONENT;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.cluster.CustomPatternGuardrails;
import com.bytechef.component.ai.agent.guardrails.cluster.KeywordGuardrails;
import com.bytechef.component.ai.agent.guardrails.cluster.PiiGuardrails;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.GuardrailsComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(GUARDRAILS_COMPONENT + "_v1_ComponentHandler")
public class GuardrailsComponentHandler implements ComponentHandler {

    public static final String GUARDRAILS_COMPONENT = "guardrails";

    private final GuardrailsComponentDefinition componentDefinition;

    public GuardrailsComponentHandler() {
        this.componentDefinition = new GuardrailsComponentDefinitionImpl(
            component(GUARDRAILS_COMPONENT)
                .title("Guardrails")
                .description("Content validation and safety guardrails for AI agents. Detect and protect against " +
                    "sensitive content, PII, and custom patterns.")
                .icon("path:assets/guardrails.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    KeywordGuardrails.of(),
                    PiiGuardrails.of(),
                    CustomPatternGuardrails.of()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class GuardrailsComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailsComponentDefinition {

        public GuardrailsComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
