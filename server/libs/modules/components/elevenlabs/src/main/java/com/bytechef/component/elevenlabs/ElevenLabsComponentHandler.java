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

package com.bytechef.component.elevenlabs;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.elevenlabs.action.ElevenLabsCreateSoundEffectAction;
import com.bytechef.component.elevenlabs.action.ElevenLabsCreateSpeechAction;
import com.bytechef.component.elevenlabs.action.ElevenLabsCreateSpeechWithTimingAction;
import com.bytechef.component.elevenlabs.action.ElevenLabsCreateTranscriptAction;
import com.bytechef.component.elevenlabs.connection.ElevenLabsConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class ElevenLabsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("elevenLabs")
        .title("ElevenLabs")
        .description(
            "ElevenLabs is an AI-powered voice synthesis company specializing in ultra-realistic text-to-speech " +
                "and voice cloning technology.")
        .icon("path:assets/elevenlabs.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(ElevenLabsConnection.CONNECTION_DEFINITION)
        .actions(
            ElevenLabsCreateSoundEffectAction.ACTION_DEFINITION,
            ElevenLabsCreateSpeechAction.ACTION_DEFINITION,
            ElevenLabsCreateSpeechWithTimingAction.ACTION_DEFINITION,
            ElevenLabsCreateTranscriptAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ElevenLabsCreateSoundEffectAction.ACTION_DEFINITION),
            tool(ElevenLabsCreateSpeechAction.ACTION_DEFINITION),
            tool(ElevenLabsCreateSpeechWithTimingAction.ACTION_DEFINITION),
            tool(ElevenLabsCreateTranscriptAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
