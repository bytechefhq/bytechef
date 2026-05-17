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

package com.bytechef.component.ai.agent.guardrails.nsfw;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.nsfw.cluster.Nsfw;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class NsfwComponentHandler implements ComponentHandler {

    public static final String NSFW = "nsfw";

    private final GuardrailComponentDefinition componentDefinition;

    public NsfwComponentHandler() {
        this.componentDefinition = new NsfwComponentDefinitionImpl(
            component(NSFW)
                .title("NSFW")
                .description("LLM-based detection of NSFW content.")
                .icon("path:assets/nsfw.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(Nsfw.of()));

        GuardrailComponentDefinition.enforceNoChildTypes(this.componentDefinition);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class NsfwComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public NsfwComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return true;
        }
    }
}
