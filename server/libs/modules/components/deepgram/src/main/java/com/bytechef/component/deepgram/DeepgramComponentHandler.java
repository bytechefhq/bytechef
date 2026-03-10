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

package com.bytechef.component.deepgram;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.deepgram.action.DeepgramRealtimeListenAction;
import com.bytechef.component.deepgram.action.DeepgramRealtimeSpeakAction;
import com.bytechef.component.deepgram.action.DeepgramVoiceAgentAction;
import com.bytechef.component.deepgram.connection.DeepgramConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class DeepgramComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("deepgram")
        .title("Deepgram")
        .description(
            "Deepgram is an AI speech platform providing real-time speech-to-text, " +
                "text-to-speech, and voice agent capabilities.")
        .icon("path:assets/deepgram.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(DeepgramConnection.CONNECTION_DEFINITION)
        .actions(
            DeepgramRealtimeListenAction.ACTION_DEFINITION,
            DeepgramRealtimeSpeakAction.ACTION_DEFINITION,
            DeepgramVoiceAgentAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
