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

import java.util.Set;

/**
 * Synthetic provider registered via {@code META-INF/services} so {@link AgentTypeRegistry} discovers it on the test
 * classpath, exercising cross-provider aggregation without coupling the test to the production agent-type catalog.
 *
 * @author Ivica Cardic
 */
public class TestAgentTypeProvider implements AgentTypeProvider {

    enum TestAgentType implements AgentType {

        ALPHA("test_alpha", false),
        BETA("test_beta", true);

        private final String key;
        private final boolean fallback;

        TestAgentType(String key, boolean fallback) {
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

    @Override
    public Set<AgentType> agentTypes() {
        return Set.of(TestAgentType.values());
    }
}
