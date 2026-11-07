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

package com.bytechef.test.voice;

import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Ivica Cardic
 */
public final class StubVoiceAgentToolset implements VoiceAgentToolset {

    private final VoiceToolDefinition definition;
    private final String result;
    private final List<String> calls = new CopyOnWriteArrayList<>();

    public StubVoiceAgentToolset(String name, String description, String schema, String result) {
        this.definition = new VoiceToolDefinition(name, description, schema);
        this.result = result;
    }

    public List<String> calls() {
        return List.copyOf(calls);
    }

    @Override
    public List<VoiceToolDefinition> definitions() {
        return List.of(definition);
    }

    @Override
    public String call(String name, String argumentsJson) {
        calls.add(name + ":" + argumentsJson);

        if (!definition.name()
            .equals(name)) {
            return VoiceAgentToolset.unknownTool(name);
        }

        return result;
    }
}
