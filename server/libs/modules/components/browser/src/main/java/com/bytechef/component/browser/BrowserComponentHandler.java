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

package com.bytechef.component.browser;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.browser.trigger.BrowserVoiceSessionTrigger;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class BrowserComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = new BrowserComponentDefinitionImpl(
        component("browser")
            .title("Browser")
            .description(
                "Triggers that surface in a customer's browser, including voice sessions answered by a voice agent.")
            .icon("path:assets/browser.svg")
            .categories(ComponentCategory.COMMUNICATION)
            .triggers(BrowserVoiceSessionTrigger.TRIGGER_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

    private static class BrowserComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ClusterRootComponentDefinition {

        private BrowserComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return List.of(VoiceAgentFunction.VOICE_AGENT, BaseToolFunction.TOOLS);
        }
    }
}
