/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactionException;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactorProvider;
import com.bytechef.platform.ai.guardrails.RedactedToolExecutionException;
import com.bytechef.platform.ai.guardrails.RedactingToolCallback;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetectors;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
class McpOutboundRedactionEnforcementTest {

    private static final String EMAIL = "bob.smith@acme.io";
    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final long GUARDED_WORKSPACE_ID = 3L;
    private static final long OTHER_WORKSPACE_ID = 4L;
    private static final ToolContext TOOL_CONTEXT = new ToolContext(Map.<String, Object>of("tenantId", "public"));

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);

    @BeforeEach
    void setUp() {
        lenient().when(settingsService.fetchSettings(anyLong()))
            .thenReturn(Optional.empty());
        lenient().when(settingsService.fetchSettings(null))
            .thenReturn(Optional.empty());
        lenient().when(settingsService.fetchEmbeddedSettings())
            .thenReturn(Optional.empty());
    }

    @Test
    void testRedactsPiiAndSecretsInAToolResultBeforeItReachesTheExternalAgent() {
        workspaceSettings(GUARDED_WORKSPACE_ID, true, true, true);

        ToolCallback toolCallback = guard(
            input -> "{\"contact\":\"" + EMAIL + "\",\"key\":\"" + AWS_KEY + "\"}", GUARDED_WORKSPACE_ID,
            McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        String result = toolCallback.call("{}", TOOL_CONTEXT);

        assertThat(result)
            .doesNotContain(EMAIL)
            .doesNotContain(AWS_KEY)
            .contains("[REDACTED_");
    }

    @Test
    void testRedactsTheMessageOfAFailingToolBeforeItBecomesToolErrorText() {
        workspaceSettings(GUARDED_WORKSPACE_ID, true, true, true);

        ToolCallback toolCallback = guard(
            input -> {
                throw new IllegalStateException("provider rejected " + EMAIL);
            },
            GUARDED_WORKSPACE_ID, McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .isInstanceOf(RedactedToolExecutionException.class)
            .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(EMAIL));
    }

    @Test
    void testLeavesTheResultUnchangedForAWorkspaceWithoutOutboundRedaction() {
        workspaceSettings(GUARDED_WORKSPACE_ID, true, true, true);

        ToolCallback toolCallback = guard(
            input -> "{\"contact\":\"" + EMAIL + "\"}", OTHER_WORKSPACE_ID,
            McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        assertThat(toolCallback.call("{}", TOOL_CONTEXT)).isEqualTo("{\"contact\":\"" + EMAIL + "\"}");
    }

    @Test
    void testLeavesTheResultUnchangedWhenTheWorkspaceRedactsPiiButNotMcpResults() {
        workspaceSettings(GUARDED_WORKSPACE_ID, true, true, false);

        ToolCallback toolCallback = guard(
            input -> "{\"contact\":\"" + EMAIL + "\"}", GUARDED_WORKSPACE_ID,
            McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        assertThat(toolCallback.call("{}", TOOL_CONTEXT)).isEqualTo("{\"contact\":\"" + EMAIL + "\"}");
    }

    @Test
    void testLeavesTheResultUnchangedWithoutTheGuardrailsProvider() {
        ToolCallback toolCallback = RedactingToolCallback.wrap(
            new StubToolCallback(input -> "{\"contact\":\"" + EMAIL + "\"}"), objectProvider(null),
            GUARDED_WORKSPACE_ID, McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        assertThat(toolCallback.call("{}", TOOL_CONTEXT)).isEqualTo("{\"contact\":\"" + EMAIL + "\"}");
    }

    @Test
    void testFailsClosedWhenTheSettingsLookupFails() {
        when(settingsService.fetchSettings(GUARDED_WORKSPACE_ID))
            .thenThrow(new IllegalStateException("connection reset"));

        ToolCallback toolCallback = guard(
            input -> "{\"contact\":\"" + EMAIL + "\"}", GUARDED_WORKSPACE_ID,
            McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .isInstanceOf(McpOutboundRedactionException.class)
            .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(EMAIL));
    }

    @Test
    void testFailsClosedWhenTheRedactorThrows() {
        workspaceSettings(GUARDED_WORKSPACE_ID, true, true, true);

        SensitiveDataRedactor failingRedactor = mock(SensitiveDataRedactor.class);

        when(failingRedactor.redact(anyString(), anySet(), anyDouble(), any()))
            .thenThrow(new IllegalStateException("detector exploded"));

        ToolCallback toolCallback = RedactingToolCallback.wrap(
            new StubToolCallback(input -> "{\"contact\":\"" + EMAIL + "\"}"),
            objectProvider(provider(failingRedactor)), GUARDED_WORKSPACE_ID,
            McpOutboundRedactorProvider.SURFACE_AUTOMATION);

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .isInstanceOf(McpOutboundRedactionException.class)
            .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(EMAIL));
    }

    @Test
    void testEmbeddedSurfaceFollowsTheEmbeddedScopeRatherThanTheTenantDefault() {
        when(settingsService.fetchSettings(null)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.PLATFORM, null, true, true, null, null, null, null, BlockingMode.BLOCK,
                null, true, null)));

        ToolCallback embeddedToolCallback = guard(
            input -> "{\"contact\":\"" + EMAIL + "\"}", null, McpOutboundRedactorProvider.SURFACE_EMBEDDED);

        assertThat(embeddedToolCallback.call("{}", TOOL_CONTEXT)).contains(EMAIL);

        when(settingsService.fetchEmbeddedSettings()).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.EMBEDDED, null, true, true, null, null, null, null, BlockingMode.BLOCK,
                null, true, null)));

        assertThat(embeddedToolCallback.call("{}", TOOL_CONTEXT)).doesNotContain(EMAIL);
    }

    private void workspaceSettings(
        long workspaceId, boolean redactPii, boolean redactSecrets, boolean redactMcpResults) {

        when(settingsService.fetchSettings(workspaceId)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, workspaceId, redactPii, redactSecrets, null, null, null, null,
                BlockingMode.BLOCK, null, redactMcpResults, null)));
    }

    private ToolCallback guard(Function<String, String> body, @Nullable Long workspaceId, String surface) {
        return RedactingToolCallback.wrap(
            new StubToolCallback(body), objectProvider(provider(new SensitiveDataRedactor(
                SensitiveDataDetectors.builtIn()))),
            workspaceId, surface);
    }

    private McpOutboundRedactorProvider provider(SensitiveDataRedactor sensitiveDataRedactor) {
        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, false, false, "", false, false, false, false);

        return new McpOutboundRedactorProviderImpl(
            aiGuardrails, sensitiveDataRedactor, objectProvider(new SimpleMeterRegistry()));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(@Nullable T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        lenient().when(objectProvider.getIfAvailable())
            .thenReturn(value);

        return objectProvider;
    }

    private record StubToolCallback(Function<String, String> body) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("lookupContact")
                .description("Looks up a contact")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return body.apply(toolInput);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            return body.apply(toolInput);
        }
    }
}
