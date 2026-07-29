/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import com.bytechef.tenant.TenantContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Decorates a workflow-backed embedded-MCP tool specification with elicitation for pending approvals — the embedded
 * counterpart of the automation server's {@code ApprovalElicitingToolSpecifications}. When the tool's synchronous run
 * pauses on a human approval (the facade returns an {@code approval_required} descriptor carrying {@code formUrl} +
 * {@code jobId}), the decorator resolves it through the strongest capability the connected client advertises:
 *
 * <ul>
 * <li><b>URL elicitation</b> — the client is pointed at the hosted approval form; once the user completed it there, the
 * tool call re-awaits the resumed run and returns its real output.</li>
 * <li><b>Form elicitation</b> (fallback) — the client collects the decision inline ({@code approved} + optional
 * {@code comment}), the server resolves the approval directly with those values and awaits the resumed run. Custom
 * approval form fields are not representable in this simple schema — workflows using them should rely on URL mode or
 * the hosted form link in the descriptor text.</li>
 * </ul>
 *
 * A run that pauses on ANOTHER approval after resuming re-elicits, bounded by {@value #MAX_ELICITATION_ROUNDS} rounds
 * per tool call. Declined/cancelled elicitations, clients without either capability, and any elicitation transport
 * failure all fall back to the plain descriptor result.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EmbeddedApprovalElicitingToolSpecifications {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedApprovalElicitingToolSpecifications.class);

    private static final int MAX_ELICITATION_ROUNDS = 3;
    private static final String STATUS_APPROVAL_REQUIRED = "approval_required";

    private EmbeddedApprovalElicitingToolSpecifications() {
    }

    public static McpServerFeatures.AsyncToolSpecification decorate(
        McpServerFeatures.AsyncToolSpecification toolSpecification, EmbeddedMcpToolFacade mcpToolFacade) {

        return new McpServerFeatures.AsyncToolSpecification(
            toolSpecification.tool(),
            (exchange, request) -> {
                // Captured on the invoking thread so the post-elicitation continuation (which lands on a reactor
                // thread) re-awaits the run under the same tenant the tool call started with.
                String tenantId = TenantContext.getCurrentTenantId();

                return toolSpecification.callHandler()
                    .apply(exchange, request)
                    .flatMap(result -> elicitApprovalIfPending(exchange, result, mcpToolFacade, tenantId, 1));
            });
    }

    private static Mono<McpSchema.CallToolResult> elicitApprovalIfPending(
        McpAsyncServerExchange exchange, McpSchema.CallToolResult result, EmbeddedMcpToolFacade mcpToolFacade,
        String tenantId, int round) {

        if (round > MAX_ELICITATION_ROUNDS) {
            return Mono.just(result);
        }

        Map<String, ?> pendingApproval = parsePendingApproval(result);

        if (pendingApproval == null || !(pendingApproval.get("jobId") instanceof Number jobId)) {
            return Mono.just(result);
        }

        if (!supportsUrlElicitation(exchange) && !supportsFormElicitation(exchange)) {
            return Mono.just(result);
        }

        String message = pendingMessage(pendingApproval);

        // Re-derive both the form URL and the resume token from the run's OWN state rather than trusting anything
        // echoed in tool-output text: a workflow can author a crafted approval_required descriptor as its normal output
        // and point the reviewer at an attacker-controlled URL (or name another run's job id). A missing resume token
        // means the job is not actually paused on an approval (stale/spoofed descriptor) → no elicitation. The DB
        // lookups run on boundedElastic to keep the reactor thread non-blocking, and under the captured tenant so they
        // can only ever name this tenant.
        return Mono
            .fromCallable(() -> TenantContext.callWithTenantId(tenantId, () -> new PendingApprovalResolution(
                mcpToolFacade.resolvePendingApprovalFormUrl(jobId.longValue())
                    .orElse(null),
                mcpToolFacade.resolvePendingApprovalResumeToken(jobId.longValue())
                    .orElse(null))))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(resolution -> {
                String formUrl = resolution.formUrl();
                String resumeToken = resolution.resumeToken();

                if (resumeToken == null) {
                    return Mono.just(result);
                }

                // URL elicitation is the strongest mode but needs the hosted approval form, so it is gated on a
                // configured public URL; form elicitation only needs the resume token and therefore still works when
                // no public URL is configured.
                Mono<McpSchema.CallToolResult> elicited = supportsUrlElicitation(exchange) && formUrl != null
                    ? elicitViaUrl(exchange, mcpToolFacade, tenantId, message, formUrl, jobId.longValue(), result)
                    : elicitViaForm(exchange, mcpToolFacade, tenantId, message, resumeToken, jobId.longValue(), result);

                // A run that pauses on a SECOND approval after resuming produces a fresh pending descriptor —
                // re-elicit it, bounded by the round counter so a long chain degrades to descriptor text.
                return elicited.flatMap(
                    nextResult -> nextResult == result
                        ? Mono.just(nextResult)
                        : elicitApprovalIfPending(exchange, nextResult, mcpToolFacade, tenantId, round + 1));
            });
    }

    private record PendingApprovalResolution(@Nullable String formUrl, @Nullable String resumeToken) {
    }

    private static Mono<McpSchema.CallToolResult> elicitViaUrl(
        McpAsyncServerExchange exchange, EmbeddedMcpToolFacade mcpToolFacade, String tenantId, String message,
        String formUrl, long jobId, McpSchema.CallToolResult fallbackResult) {

        McpSchema.ElicitRequest elicitRequest = McpSchema.ElicitUrlRequest
            .builder(message, formUrl, "approval-" + jobId)
            .build();

        return exchange.createElicitation(elicitRequest)
            .flatMap(elicitResult -> elicitResult.action() == McpSchema.ElicitResult.Action.ACCEPT
                ? runOnBoundedElastic(() -> mcpToolFacade.awaitApprovedWorkflowRun(jobId), tenantId)
                : Mono.just(fallbackResult))
            .onErrorResume(exception -> {
                log.warn("Approval URL elicitation failed; returning the pending descriptor: {}",
                    exception.getMessage());

                return Mono.just(fallbackResult);
            });
    }

    private static Mono<McpSchema.CallToolResult> elicitViaForm(
        McpAsyncServerExchange exchange, EmbeddedMcpToolFacade mcpToolFacade, String tenantId, String message,
        String resumeToken, long jobId, McpSchema.CallToolResult fallbackResult) {

        McpSchema.ElicitRequest elicitRequest = McpSchema.ElicitFormRequest
            .builder(message, decisionSchema())
            .build();

        return exchange.createElicitation(elicitRequest)
            .flatMap(elicitResult -> {
                if (elicitResult.action() != McpSchema.ElicitResult.Action.ACCEPT ||
                    elicitResult.content() == null) {

                    return Mono.just(fallbackResult);
                }

                Map<String, Object> data = new HashMap<>();

                data.put("approved", Boolean.TRUE.equals(elicitResult.content()
                    .get("approved")));

                if (elicitResult.content()
                    .get("comment") instanceof String comment && !comment.isBlank()) {

                    data.put("comment", comment);
                }

                return runOnBoundedElastic(
                    () -> mcpToolFacade.resolveApprovalAndAwait(resumeToken, data, jobId), tenantId);
            })
            .onErrorResume(exception -> {
                log.warn("Approval form elicitation failed; returning the pending descriptor: {}",
                    exception.getMessage());

                return Mono.just(fallbackResult);
            });
    }

    private static Mono<McpSchema.CallToolResult> runOnBoundedElastic(
        java.util.concurrent.Callable<@Nullable Object> callable, String tenantId) {

        return Mono
            .fromCallable(() -> TenantContext.callWithTenantId(tenantId, () -> {
                try {
                    return callable.call();
                } catch (RuntimeException runtimeException) {
                    throw runtimeException;
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }))
            .subscribeOn(Schedulers.boundedElastic())
            .map(output -> McpSchema.CallToolResult.builder()
                .addTextContent(output == null ? "" : JsonUtils.write(output))
                .isError(false)
                .build())
            // A failure of the resumed RUN (a workflow error after approval) must surface as a tool error, not be
            // swallowed by the caller's onErrorResume and reported as "returning the pending descriptor" — that would
            // re-prompt the reviewer for an approval that was already resolved. Only elicitation-TRANSPORT failures
            // (from exchange.createElicitation) should degrade to the pending descriptor.
            .onErrorResume(exception -> {
                log.warn("Awaiting the resumed run after approval failed: {}", exception.getMessage());

                return Mono.just(McpSchema.CallToolResult.builder()
                    .addTextContent(
                        "The workflow run failed after the approval was resolved: " + exception.getMessage())
                    .isError(true)
                    .build());
            });
    }

    /**
     * The inline decision schema for FORM-mode elicitation: the reviewer's approve/discard choice plus an optional
     * comment — matching the field-less approval card. Custom approval form fields are out of scope for this schema.
     */
    private static Map<String, Object> decisionSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "approved", Map.of(
                    "type", "boolean",
                    "title", "Approve",
                    "description", "true approves the pending action, false discards it"),
                "comment", Map.of(
                    "type", "string",
                    "title", "Comment",
                    "description", "Optional note passed back to the workflow on either outcome")),
            "required", java.util.List.of("approved"));
    }

    private static String pendingMessage(Map<String, ?> pendingApproval) {
        Object pendingMessage = pendingApproval.get("message");

        return pendingMessage == null
            ? "Approval required — resolve the pending approval to continue."
            : String.valueOf(pendingMessage);
    }

    /**
     * Returns the parsed {@code approval_required} descriptor when the result carries one as its first text content,
     * otherwise {@code null}. Non-JSON and non-descriptor payloads (the overwhelmingly common case) return null cheaply
     * — the text must at least mention the status marker before a parse is attempted.
     */
    private static @Nullable Map<String, ?> parsePendingApproval(McpSchema.CallToolResult result) {
        if (Boolean.TRUE.equals(result.isError()) || result.content() == null || result.content()
            .isEmpty()) {

            return null;
        }

        if (!(result.content()
            .getFirst() instanceof McpSchema.TextContent textContent)) {

            return null;
        }

        String text = textContent.text();

        if (text == null || !text.contains(STATUS_APPROVAL_REQUIRED)) {
            return null;
        }

        try {
            Map<String, ?> map = JsonUtils.readMap(text);

            return STATUS_APPROVAL_REQUIRED.equals(map.get("status")) ? map : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean supportsUrlElicitation(McpAsyncServerExchange exchange) {
        McpSchema.ClientCapabilities.Elicitation elicitation = elicitationCapability(exchange);

        return elicitation != null && elicitation.url() != null;
    }

    private static boolean supportsFormElicitation(McpAsyncServerExchange exchange) {
        McpSchema.ClientCapabilities.Elicitation elicitation = elicitationCapability(exchange);

        return elicitation != null && elicitation.form() != null;
    }

    private static McpSchema.ClientCapabilities.@Nullable Elicitation elicitationCapability(
        McpAsyncServerExchange exchange) {

        McpSchema.ClientCapabilities clientCapabilities = exchange.getClientCapabilities();

        return clientCapabilities == null ? null : clientCapabilities.elicitation();
    }
}
