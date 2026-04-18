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

package com.bytechef.component.ai.agent.guardrails.topicalalignment;

import static com.bytechef.component.ai.agent.guardrails.topicalalignment.TopicalAlignmentComponentHandler.TOPICAL_ALIGNMENT;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.topicalalignment.cluster.TopicalAlignment;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TOPICAL_ALIGNMENT + "_v1_ComponentHandler")
public class TopicalAlignmentComponentHandler implements ComponentHandler {

    public static final String TOPICAL_ALIGNMENT = "topicalAlignment";

    private final GuardrailComponentDefinition componentDefinition;

    public TopicalAlignmentComponentHandler(TopicalAlignment topicalAlignment) {
        this.componentDefinition = new TopicalAlignmentComponentDefinitionImpl(
            component(TOPICAL_ALIGNMENT)
                .title("Topical Alignment")
                .description("LLM-based check that input stays within an allowed topic.")
                .icon("path:assets/topical-alignment.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(topicalAlignment.of()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class TopicalAlignmentComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public TopicalAlignmentComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return true;
        }
    }
}
