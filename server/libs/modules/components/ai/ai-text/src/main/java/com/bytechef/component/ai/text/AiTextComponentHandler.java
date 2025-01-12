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

package com.bytechef.component.ai.text;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.platform.component.definition.AiComponentDefinition.AI_TEXT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.text.action.ClassifyTextAction;
import com.bytechef.component.ai.text.action.ScoreAction;
import com.bytechef.component.ai.text.action.SentimentAction;
import com.bytechef.component.ai.text.action.SimilaritySearchAction;
import com.bytechef.component.ai.text.action.SummarizeTextAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.AiComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component(AI_TEXT + "_v1_ComponentHandler")
public class AiTextComponentHandler implements ComponentHandler {

    private final AiComponentDefinition componentDefinition;

    public AiTextComponentHandler(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        this.componentDefinition = new AiTextComponentDefinitionImpl(ai.getComponent());
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AiTextComponentDefinitionImpl
        extends AbstractComponentDefinitionWrapper implements AiComponentDefinition {

        private AiTextComponentDefinitionImpl(ApplicationProperties.Ai.Component component) {
            super(
                component(AI_TEXT)
                    .title("AI Text")
                    .description("AI Helper component for text analysis and generation.")
                    .icon("path:assets/ai-text.svg")
                    .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                    .actions(
                        new ClassifyTextAction(component).actionDefinition,
                        new SentimentAction(component).actionDefinition,
                        new ScoreAction(component).actionDefinition,
                        new SummarizeTextAction(component).actionDefinition,
                        new SimilaritySearchAction(component).actionDefinition));
        }
    }
}
