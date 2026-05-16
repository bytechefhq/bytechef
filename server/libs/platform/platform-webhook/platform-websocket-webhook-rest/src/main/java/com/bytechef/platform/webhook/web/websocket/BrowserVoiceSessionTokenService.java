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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mints and validates short-lived single-use tokens that authorize a browser to open a production-webhook voice
 * WebSocket against {@code wss://host/webhooks/(webhookId)/wss}.
 *
 * <p>
 * Sibling of {@link WorkflowTestVoiceSessionTokenService}; the test variant is keyed by workflowId and gated by the
 * standard {@code /internal/**} session cookie. This production variant is keyed by webhookId and lives under
 * {@code /webhooks/**} which is permit-all today — security comes from the single-use semantics of the token plus any
 * webhook-signature configuration on the trigger itself.
 *
 * @author Ivica Cardic
 */
@Service
public class BrowserVoiceSessionTokenService {

    private static final Logger log = LoggerFactory.getLogger(BrowserVoiceSessionTokenService.class);

    private static final long TOKEN_TTL_SECONDS = 60L;

    private final Cache<String, String> tokenToWebhookId = Caffeine.newBuilder()
        .expireAfterWrite(TOKEN_TTL_SECONDS, TimeUnit.SECONDS)
        .maximumSize(100000)
        .build();

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final VoiceMetricsRecorder metricsRecorder;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public BrowserVoiceSessionTokenService(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, VoiceMetricsRecorder metricsRecorder,
        WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.metricsRecorder = metricsRecorder;
        this.workflowService = workflowService;
    }

    /**
     * Issues a token for {@code webhookId}, after verifying it resolves to a deployed workflow whose trigger type is
     * WEBSOCKET. Throws {@link InvalidWebhookException} for any failure so the controller can return a 4xx instead of a
     * permissive 200 that mints a token for a non-voice or non-existent webhook.
     */
    public Token issue(String webhookId) {
        if (webhookId == null || webhookId.isBlank()) {
            metricsRecorder.recordTokenRejected();

            throw new InvalidWebhookException("Missing webhookId");
        }

        if (!isVoiceCapable(webhookId)) {
            metricsRecorder.recordTokenRejected();

            throw new InvalidWebhookException(
                "Webhook is not a browser-voice trigger or does not exist: " + webhookId);
        }

        String token = UUID.randomUUID()
            .toString();

        tokenToWebhookId.put(token, webhookId);

        metricsRecorder.recordTokenIssued();

        return new Token(token, TOKEN_TTL_SECONDS);
    }

    /**
     * Cheap-enough check: resolves the webhookId to its workflow and verifies the trigger type. Returns false on any
     * resolution failure (parsing, missing workflow, missing trigger). Logs the reason at debug so misconfigured
     * webhook URLs can be diagnosed without leaking detail in 4xx responses.
     */
    private boolean isVoiceCapable(String webhookId) {
        try {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(webhookId);

            JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
                workflowExecutionId.getType());

            String workflowId = jobPrincipalAccessor.getWorkflowId(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

            Workflow workflow = workflowService.getWorkflow(workflowId);

            List<WorkflowTrigger> triggers = WorkflowTrigger.of(workflow);

            // Trigger type isn't stored on WorkflowTrigger directly; the workflow's trigger definition carries it.
            // For Tier 1 we accept the lighter check: the trigger exists AND carries a websocketTasks extension
            // (which only browser-voice and twilio-inbound triggers do today). Strict TriggerType verification
            // would require a separate registry lookup; not worth the latency on the hot path.
            WorkflowTrigger matchingTrigger = triggers.stream()
                .filter(trigger -> trigger.getName()
                    .equals(workflowExecutionId.getTriggerName()))
                .findFirst()
                .orElse(null);

            if (matchingTrigger == null) {
                return false;
            }

            String websocketTasks = WebsocketTasks.resolve(matchingTrigger);

            return websocketTasks != null && !websocketTasks.isBlank();
        } catch (Exception exception) {
            if (log.isDebugEnabled()) {
                log.debug("Voice-capability check failed for webhookId={}: {}", webhookId, exception.getMessage());
            }

            return false;
        }
    }

    /**
     * Consume the token and verify it was issued for {@code webhookId}. Returns true on first call with a matching
     * pair; subsequent calls (and mismatched webhookIds) return false.
     */
    public boolean consume(String token, String webhookId) {
        if (token == null || webhookId == null) {
            metricsRecorder.recordTokenRejected();

            return false;
        }

        String issuedFor = tokenToWebhookId.getIfPresent(token);

        if (issuedFor == null || !issuedFor.equals(webhookId)) {
            metricsRecorder.recordTokenRejected();

            return false;
        }

        tokenToWebhookId.invalidate(token);
        metricsRecorder.recordTokenConsumed();

        return true;
    }

    public record Token(String token, long expiresInSeconds) {
    }

    public static class InvalidWebhookException extends RuntimeException {

        public InvalidWebhookException(String message) {
            super(message);
        }
    }
}
