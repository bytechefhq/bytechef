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

package com.bytechef.component.ai.agent.guardrails.jailbreak;

import static com.bytechef.component.ai.agent.guardrails.jailbreak.JailbreakComponentHandler.JAILBREAK;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.jailbreak.cluster.Jailbreak;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(JAILBREAK + "_v1_ComponentHandler")
public class JailbreakComponentHandler implements ComponentHandler {

    public static final String JAILBREAK = "jailbreak";

    private final GuardrailComponentDefinition componentDefinition;

    public JailbreakComponentHandler(Jailbreak jailbreak) {
        this.componentDefinition = new JailbreakComponentDefinitionImpl(
            component(JAILBREAK)
                .title("Jailbreak")
                .description("LLM-based detection of jailbreak / prompt-injection attempts.")
                .icon("path:assets/jailbreak.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(jailbreak.of()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class JailbreakComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public JailbreakComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return true;
        }
    }
}
