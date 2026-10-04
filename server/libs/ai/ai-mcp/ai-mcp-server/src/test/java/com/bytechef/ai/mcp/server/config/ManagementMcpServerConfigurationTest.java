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

package com.bytechef.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Hooks;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerConfigurationTest {

    private volatile boolean automaticContextPropagationEnabledBeforeTest;

    @BeforeEach
    void captureAutomaticContextPropagationState() {
        automaticContextPropagationEnabledBeforeTest = Hooks.isAutomaticContextPropagationEnabled();
    }

    @AfterEach
    void restoreAutomaticContextPropagationState() {
        if (automaticContextPropagationEnabledBeforeTest) {
            Hooks.enableAutomaticContextPropagation();
        } else {
            Hooks.disableAutomaticContextPropagation();
        }

        SecurityContextHolder.clearContext();
    }

    @Test
    void securityContextDoesNotCrossWhenAutomaticPropagationDisabled() {
        Hooks.disableAutomaticContextPropagation();

        setAdminSecurityContext();

        ProbeResult probeResult = invokeProbe();

        assertThat(probeResult.threadName())
            .as("the thread hop itself must still happen - only the context capture is under test here")
            .startsWith("boundedElastic");
        assertThat(probeResult.authentication())
            .as("without the hook the SecurityContext must NOT survive the hop - this is what makes the positive "
                + "test in ReactorContextPropagationConfigurationTest a real pin rather than a tautology")
            .isNull();
    }

    private static void setAdminSecurityContext() {
        Authentication adminAuthentication = new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(adminAuthentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private static ProbeResult invokeProbe() {
        AtomicReference<ProbeResult> capturedResult = new AtomicReference<>();

        ToolCallback probe = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                    .name("probe")
                    .description("Records the thread name and SecurityContext it executes under.")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                capturedResult.set(
                    new ProbeResult(
                        Thread.currentThread()
                            .getName(),
                        SecurityContextHolder.getContext()
                            .getAuthentication()));

                return "{}";
            }
        };

        McpServerFeatures.AsyncToolSpecification asyncToolSpecification =
            McpToolUtils.toAsyncToolSpecification(probe);

        asyncToolSpecification.callHandler()
            .apply(
                mock(McpAsyncServerExchange.class),
                McpSchema.CallToolRequest.builder()
                    .name("probe")
                    .arguments(Map.of())
                    .build())
            .block();

        return capturedResult.get();
    }

    private record ProbeResult(String threadName, @Nullable Authentication authentication) {
    }
}
