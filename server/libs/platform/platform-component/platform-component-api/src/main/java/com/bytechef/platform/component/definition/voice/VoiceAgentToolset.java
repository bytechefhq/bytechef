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

package com.bytechef.platform.component.definition.voice;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface VoiceAgentToolset {

    VoiceAgentToolset EMPTY = new VoiceAgentToolset() {

        @Override
        public List<VoiceToolDefinition> definitions() {
            return List.of();
        }

        @Override
        public String call(String name, String argumentsJson) {
            return unknownTool(name);
        }
    };

    static String unknownTool(String name) {
        return "Unknown tool '" + name + "'. Tell the user this action is not available.";
    }

    List<VoiceToolDefinition> definitions();

    String call(String name, String argumentsJson);
}
