/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactor;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactorProvider;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedApprovalElicitingToolSpecificationsTest {

    private static final McpSchema.Tool TOOL = McpSchema.Tool.builder()
        .name("run_workflow")
        .description("Runs the workflow")
        .inputSchema(Map.of("type", "object"))
        .build();

    private static final String SURFACE = "mcp_embedded";

    private final EmbeddedMcpToolFacade mcpToolFacade = mock(EmbeddedMcpToolFacade.class);
    private final McpAsyncServerExchange exchange = mock(McpAsyncServerExchange.class);

    private ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider;

    @BeforeEach
    void setUp() {
        mcpOutboundRedactorProviderProvider = emptyProvider();
    }

    @Test
    void testOrdinaryResultPassesThroughWithoutElicitation() {
        McpSchema.CallToolResult innerResult = textResult("{\"answer\": 42}");

        McpSchema.CallToolResult result = call(innerResult);

        assertThat(result).isSameAs(innerResult);

        verify(exchange, never()).createElicitation(any());
    }

    @Test
    void testAcceptedElicitationReturnsTheResumedRunOutput() {
        stubServerSideResolution();
        stubUrlCapability();

        when(exchange.createElicitation(any())).thenReturn(
            Mono.just(
                McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                    .build()));
        when(mcpToolFacade.awaitApprovedWorkflowRun(42L)).thenReturn(Map.of("message", "done"));

        McpSchema.CallToolResult result = call(pendingApprovalResult());

        verify(mcpToolFacade).awaitApprovedWorkflowRun(42L);

        assertThat(firstText(result)).contains("done");
        assertThat(firstText(result)).doesNotContain("approval_required");
    }

    @Test
    void testAcceptedElicitationRedactsTheResumedRunOutput() {
        stubServerSideResolution();
        stubUrlCapability();

        when(exchange.createElicitation(any())).thenReturn(
            Mono.just(
                McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                    .build()));
        when(mcpToolFacade.awaitApprovedWorkflowRun(42L))
            .thenReturn(Map.of("message", "contact bob@acme.io"));

        mcpOutboundRedactorProviderProvider =
            providerReturning(text -> text.replace("bob@acme.io", "[REDACTED_EMAIL_ADDRESS]"));

        McpSchema.CallToolResult result = call(pendingApprovalResult());

        assertThat(firstText(result)).contains("[REDACTED_EMAIL_ADDRESS]");
        assertThat(firstText(result)).doesNotContain("bob@acme.io");
    }

    @Test
    void testResumedRunOutputFailsClosedWhenRedactionThrows() {
        stubServerSideResolution();
        stubUrlCapability();

        when(exchange.createElicitation(any())).thenReturn(
            Mono.just(
                McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                    .build()));
        when(mcpToolFacade.awaitApprovedWorkflowRun(42L))
            .thenReturn(Map.of("message", "contact bob@acme.io"));

        mcpOutboundRedactorProviderProvider = providerReturning(text -> {
            throw new IllegalStateException("detector exploded");
        });

        McpSchema.CallToolResult result = call(pendingApprovalResult());

        assertThat(result.isError())
            .as("the raw resumed-run output must never be returned when redaction fails")
            .isTrue();
        assertThat(firstText(result)).doesNotContain("bob@acme.io");
    }

    @Test
    void testRedactsTheFailureMessageOfARunThatFailedAfterApproval() {
        stubServerSideResolution();
        stubUrlCapability();

        when(exchange.createElicitation(any())).thenReturn(
            Mono.just(
                McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                    .build()));
        when(mcpToolFacade.awaitApprovedWorkflowRun(42L))
            .thenThrow(new IllegalStateException("the provider rejected contact bob@acme.io"));

        mcpOutboundRedactorProviderProvider =
            providerReturning(text -> text.replace("bob@acme.io", "[REDACTED_EMAIL_ADDRESS]"));

        McpSchema.CallToolResult result = call(pendingApprovalResult());

        assertThat(result.isError()).isTrue();
        assertThat(firstText(result))
            .as("a run failure's message is as payload-derived as its output")
            .contains("[REDACTED_EMAIL_ADDRESS]")
            .doesNotContain("bob@acme.io");
    }

    @Test
    void testDropsTheFailureMessageWhenRedactingItFails() {
        stubServerSideResolution();
        stubUrlCapability();

        when(exchange.createElicitation(any())).thenReturn(
            Mono.just(
                McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                    .build()));
        when(mcpToolFacade.awaitApprovedWorkflowRun(42L))
            .thenThrow(new IllegalStateException("the provider rejected contact bob@acme.io"));

        mcpOutboundRedactorProviderProvider = providerReturning(text -> {
            throw new IllegalStateException("detector exploded");
        });

        McpSchema.CallToolResult result = call(pendingApprovalResult());

        assertThat(result.isError())
            .as("a run failure must still surface as a tool error")
            .isTrue();
        assertThat(firstText(result))
            .as("the raw failure message must never be the fallback when redacting it fails")
            .contains("The workflow run failed after the approval was resolved")
            .doesNotContain("bob@acme.io");
    }

    @Test
    void testDeclinedElicitationReturnsThePendingDescriptor() {
        stubUrlCapability();

        when(exchange.createElicitation(any())).thenReturn(
            Mono.just(
                McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.DECLINE)
                    .build()));

        McpSchema.CallToolResult innerResult = pendingApprovalResult();

        McpSchema.CallToolResult result = call(innerResult);

        assertThat(result).isSameAs(innerResult);

        verify(mcpToolFacade, never()).awaitApprovedWorkflowRun(42L);
    }

    private McpSchema.CallToolResult call(McpSchema.CallToolResult innerResult) {
        McpServerFeatures.AsyncToolSpecification innerSpecification = new McpServerFeatures.AsyncToolSpecification(
            TOOL, (currentExchange, request) -> Mono.just(innerResult));

        McpServerFeatures.AsyncToolSpecification decorated = EmbeddedApprovalElicitingToolSpecifications.decorate(
            innerSpecification, mcpToolFacade, mcpOutboundRedactorProviderProvider, null, SURFACE);

        return decorated.callHandler()
            .apply(exchange, new McpSchema.CallToolRequest("run_workflow", Map.of()))
            .block();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpOutboundRedactorProvider> emptyProvider() {
        ObjectProvider<McpOutboundRedactorProvider> providerProvider = mock(ObjectProvider.class);

        when(providerProvider.getIfAvailable()).thenReturn(null);

        return providerProvider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpOutboundRedactorProvider> providerReturning(McpOutboundRedactor redactor) {
        ObjectProvider<McpOutboundRedactorProvider> providerProvider = mock(ObjectProvider.class);

        when(providerProvider.getIfAvailable())
            .thenReturn((workspaceId, surface) -> Optional.of(redactor));

        return providerProvider;
    }

    private void stubServerSideResolution() {
        when(mcpToolFacade.resolvePendingApprovalFormUrl(42L))
            .thenReturn(Optional.of("https://example.com/resume/tok"));
        when(mcpToolFacade.resolvePendingApprovalResumeToken(42L))
            .thenReturn(Optional.of("tok"));
    }

    private void stubUrlCapability() {
        when(exchange.getClientCapabilities()).thenReturn(
            McpSchema.ClientCapabilities.builder()
                .elicitation(
                    new McpSchema.ClientCapabilities.Elicitation(
                        null, new McpSchema.ClientCapabilities.Elicitation.Url()))
                .build());
    }

    private static McpSchema.CallToolResult pendingApprovalResult() {
        return textResult(
            JsonUtils.write(
                Map.of(
                    "status", "approval_required",
                    "message", "Approval required — resolve it at: https://example.com/resume/tok",
                    "formUrl", "https://example.com/resume/tok",
                    "jobId", 42L)));
    }

    private static McpSchema.CallToolResult textResult(String text) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(text)
            .isError(false)
            .build();
    }

    private static String firstText(McpSchema.CallToolResult result) {
        McpSchema.TextContent textContent = (McpSchema.TextContent) result.content()
            .getFirst();

        return textContent.text();
    }
}
