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

package com.bytechef.component.ai.agent.guardrails.llmpii;

import static com.bytechef.component.ai.agent.guardrails.llmpii.LlmPiiComponentHandler.LLM_PII;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.llmpii.cluster.LlmPii;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(LLM_PII + "_v1_ComponentHandler")
public class LlmPiiComponentHandler implements ComponentHandler {

    public static final String LLM_PII = "llmPii";

    private final GuardrailComponentDefinition componentDefinition;

    public LlmPiiComponentHandler() {
        this.componentDefinition = new LlmPiiComponentDefinitionImpl(
            component(LLM_PII)
                .title("LLM PII")
                .description("LLM-assisted detection and masking of PII (names, addresses, emails, etc.).")
                .icon("path:assets/llm-pii.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    LlmPii.ofCheck(),
                    LlmPii.ofSanitize()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class LlmPiiComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public LlmPiiComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return true;
        }
    }
}
