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

package com.bytechef.ai.agent.tool;

/**
 * Edition-neutral agent types owned by the core module. Currently just the universal {@link #UNKNOWN} fallback returned
 * by {@link AgentTypeRegistry#fromKey(String)} when a key matches no contributed {@link AgentType}.
 *
 * @author Ivica Cardic
 */
public enum CoreAgentType implements AgentType {

    UNKNOWN("unknown", true);

    private final String key;
    private final boolean fallback;

    CoreAgentType(String key, boolean fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return fallback;
    }
}
