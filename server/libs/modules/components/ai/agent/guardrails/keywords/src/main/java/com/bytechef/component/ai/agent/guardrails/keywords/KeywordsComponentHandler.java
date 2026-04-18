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

package com.bytechef.component.ai.agent.guardrails.keywords;

import static com.bytechef.component.ai.agent.guardrails.keywords.KeywordsComponentHandler.KEYWORDS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.keywords.cluster.Keywords;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(KEYWORDS + "_v1_ComponentHandler")
public class KeywordsComponentHandler implements ComponentHandler {

    public static final String KEYWORDS = "keywords";

    private final GuardrailComponentDefinition componentDefinition;

    public KeywordsComponentHandler() {
        this.componentDefinition = new KeywordsComponentDefinitionImpl(
            component(KEYWORDS)
                .title("Keywords")
                .description("Flags the input if any listed keyword appears in it.")
                .icon("path:assets/keywords.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(Keywords.of()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class KeywordsComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public KeywordsComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return false;
        }
    }
}
