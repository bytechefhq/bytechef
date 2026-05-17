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

package com.bytechef.component.ai.agent.guardrails.sanitizetext;

import static com.bytechef.component.ai.agent.guardrails.sanitizetext.SanitizeTextComponentHandler.SANITIZE_TEXT;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.sanitizetext.cluster.SanitizeText;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.SanitizeTextComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(SANITIZE_TEXT + "_v1_ComponentHandler")
public class SanitizeTextComponentHandler implements ComponentHandler {

    public static final String SANITIZE_TEXT = "sanitizeText";

    private final SanitizeTextComponentDefinition componentDefinition;

    public SanitizeTextComponentHandler(SanitizeText sanitizeText) {
        this.componentDefinition = new SanitizeTextComponentDefinitionImpl(
            component(SANITIZE_TEXT)
                .title("Sanitize Text")
                .description("Runs configured sanitizers on the model response.")
                .icon("path:assets/sanitize-text.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(sanitizeText.of()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class SanitizeTextComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements SanitizeTextComponentDefinition {

        public SanitizeTextComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
