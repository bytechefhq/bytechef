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

package com.bytechef.platform.ai.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;

/**
 * @author Ivica Cardic
 */
public enum PlatformAgentType implements AgentType {

    JSON_SCHEMA_BUILDER_ASK(Source.JSON_SCHEMA_BUILDER, Mode.ASK),
    JSON_SCHEMA_BUILDER_BUILD(Source.JSON_SCHEMA_BUILDER, Mode.BUILD),
    SAMPLE_OUTPUT_ASK(Source.SAMPLE_OUTPUT, Mode.ASK),
    SAMPLE_OUTPUT_BUILD(Source.SAMPLE_OUTPUT, Mode.BUILD);

    private final String key;

    PlatformAgentType(Source source, Mode mode) {
        this(source.agentId(mode));
    }

    PlatformAgentType(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return false;
    }
}
