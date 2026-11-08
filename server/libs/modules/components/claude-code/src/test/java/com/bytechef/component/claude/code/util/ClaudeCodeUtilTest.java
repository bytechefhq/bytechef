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

package com.bytechef.component.claude.code.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClaudeCodeUtilTest {

    private final Context context = mock(Context.class);
    private final Context.Json json = mock(Context.Json.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        when(context.json(any())).thenAnswer(
            invocation -> ((Context.ContextFunction<Context.Json, ?>) invocation.getArgument(0)).apply(json));
        when(json.write(any())).thenAnswer(invocation -> JsonUtils.write(invocation.getArgument(0)));
        when(json.read(any(String.class), any(Class.class))).thenAnswer(
            invocation -> JsonUtils.read((String) invocation.getArgument(0), (Class<?>) invocation.getArgument(1)));
    }

    @Test
    void testDeserializeMcpServersReturnsHttpConfigsKeyedByLabel() {
        String github = ClaudeCodeUtil.serializeMcpServer(
            "github", "https://mcp.example.com/github", Map.of("Authentication", "Bearer token"), context);
        String jira = ClaudeCodeUtil.serializeMcpServer("jira", "https://mcp.example.com/jira", Map.of(), context);

        Map<String, McpServerConfig> mcpServerConfigs = ClaudeCodeUtil.deserializeMcpServers(
            List.of(github, jira), context);

        assertThat(mcpServerConfigs).containsOnlyKeys("github", "jira");
        assertThat(mcpServerConfigs.get("github")).isEqualTo(
            new McpServerConfig.McpHttpServerConfig(
                "https://mcp.example.com/github", Map.of("Authentication", "Bearer token")));
        assertThat(mcpServerConfigs.get("jira")).isEqualTo(
            new McpServerConfig.McpHttpServerConfig("https://mcp.example.com/jira", Map.of()));
    }

    @Test
    void testDeserializeMcpServersReturnsEmptyMapForNoServers() {
        assertThat(ClaudeCodeUtil.deserializeMcpServers(List.of(), context)).isEmpty();
    }
}
