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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Hooks;

/**
 * Pins the one thread hand-off the management MCP tool-execution path depends on to see the caller's authenticated
 * {@code SecurityContext}, and therefore for {@code @PreAuthorize} guards (e.g. {@code ContextStoreSourceFacadeImpl},
 * {@code ContextStoreFacadeImpl}) on tools reached through this server to actually enforce anything.
 *
 * <p>
 * {@code McpToolUtils.toAsyncToolSpecification} wraps every synchronous tool callback in
 * {@code Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())}, moving execution off the servlet thread the
 * security filter chain populated onto a pooled {@code boundedElastic} worker. {@code SecurityContextHolder} is plain
 * {@code MODE_THREADLOCAL} (no inheritable/inheritable-thread-local strategy is configured anywhere in this repo), so
 * that move only carries the {@code SecurityContext} across because two things happen to combine: (1)
 * {@code ReactorContextPropagationConfiguration} (in {@code platform-configuration-service}) calls
 * {@code Hooks.enableAutomaticContextPropagation()}, which installs a {@code Schedulers.onScheduleHook} that snapshots
 * every registered {@code ThreadLocalAccessor} at {@code schedule()} time and restores it on the worker thread; and (2)
 * Spring Security auto-registers {@code SecurityContextHolderThreadLocalAccessor} via {@code ServiceLoader}, so
 * {@code ContextRegistry} picks it up automatically. Full trace:
 * {@code .superpowers/sdd/2026-08-18-uniform-tool-surface-open-work/task-E-report.md}.
 *
 * <p>
 * <b>Why this would otherwise be invisible.</b> This module ({@code ai-mcp-server}) declares no dependency on
 * {@code platform-configuration-service} — the module that installs the hook. Nothing in the build graph forces the
 * hook to exist wherever this server is hosted; it only works today because {@code server-app}, the sole current host
 * of the management MCP server, happens to carry both modules and component-scans the enabling {@code @Configuration}.
 * A future host assembled the way this repo's own convention describes for distributed EE apps — {@code *-api} +
 * {@code *-remote-client} WITHOUT {@code *-service} — would silently drop the hook: every {@code @PreAuthorize}-guarded
 * MCP tool would then deny everyone, admins included, fail-closed, with no compile error and no other test catching the
 * regression. This test pins the boundary directly, independent of that module coupling, so a change that breaks
 * propagation fails here first.
 *
 * @author Ivica Cardic
 */
class ManagementMcpServerSecurityContextPropagationTest {

    // Written in @BeforeEach and read back in @AfterEach. JUnit does not promise those two callbacks run on the
    // same thread, and this test deliberately exercises reactor's boundedElastic pool, so the read needs the
    // happens-before a volatile write gives it - otherwise the restore below can act on a stale value and leave
    // the process-global Hooks state flipped for every later test in the JVM.
    private volatile boolean automaticContextPropagationEnabledBeforeTest;

    @BeforeEach
    void captureAutomaticContextPropagationState() {
        automaticContextPropagationEnabledBeforeTest = Hooks.isAutomaticContextPropagationEnabled();
    }

    @AfterEach
    void restoreAutomaticContextPropagationState() {
        // Hooks.enableAutomaticContextPropagation() is process-global (a static Schedulers.onScheduleHook). Leaving
        // it flipped after this test would corrupt every other test in the same JVM in a way that looks like an
        // unrelated flaky failure, so restore whatever state existed before this test ran regardless of outcome.
        if (automaticContextPropagationEnabledBeforeTest) {
            Hooks.enableAutomaticContextPropagation();
        } else {
            Hooks.disableAutomaticContextPropagation();
        }

        SecurityContextHolder.clearContext();
    }

    @Test
    void securityContextCrossesToBoundedElasticWorkerWhenAutomaticPropagationEnabled() {
        Hooks.enableAutomaticContextPropagation();

        setAdminSecurityContext();

        ProbeResult probeResult = invokeProbe();

        assertThat(probeResult.threadName())
            .as("the probe must actually hop onto a boundedElastic worker, or the assertion below is vacuous")
            .startsWith("boundedElastic");
        assertThat(probeResult.authentication()).isNotNull();
        assertThat(probeResult.authentication()
            .getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
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
                + "test above a real pin rather than a tautology")
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
