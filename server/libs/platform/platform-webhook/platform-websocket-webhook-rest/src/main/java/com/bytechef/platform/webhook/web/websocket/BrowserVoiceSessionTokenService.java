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
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnWebhook
public class BrowserVoiceSessionTokenService {

    private static final Logger log = LoggerFactory.getLogger(BrowserVoiceSessionTokenService.class);

    private static final String VOICE_SESSION_TRIGGER_TYPE = "browser/v1/voiceSession";

    private static final long TOKEN_TTL_SECONDS = 60L;

    private final Cache<String, String> tokenToWebhookId = Caffeine.newBuilder()
        .expireAfterWrite(TOKEN_TTL_SECONDS, TimeUnit.SECONDS)
        .maximumSize(100000)
        .build();

    private final ObjectProvider<ContextFactory> contextFactoryProvider;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final VoiceMetricsRecorder metricsRecorder;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public BrowserVoiceSessionTokenService(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, VoiceMetricsRecorder metricsRecorder,
        WorkflowService workflowService, ObjectProvider<ContextFactory> contextFactoryProvider) {

        this.contextFactoryProvider = contextFactoryProvider;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.metricsRecorder = metricsRecorder;
        this.workflowService = workflowService;
    }

    public Token issue(String webhookId) {
        if (webhookId == null || webhookId.isBlank()) {
            metricsRecorder.recordTokenRejected();

            throw new InvalidWebhookException("Missing webhookId");
        }

        if (contextFactoryProvider.getIfAvailable() == null) {
            metricsRecorder.recordTokenRejected();

            throw new VoiceUnavailableException(
                "Browser voice sessions are not available on this node; route voice traffic to a node that runs " +
                    "the full component runtime");
        }

        WorkflowExecutionId workflowExecutionId;

        try {
            workflowExecutionId = WorkflowExecutionId.parse(webhookId);
        } catch (RuntimeException runtimeException) {
            metricsRecorder.recordTokenRejected();

            throw new InvalidWebhookException(
                "Webhook is not a browser-voice trigger or does not exist: " + webhookId);
        }

        Eligibility eligibility = TenantContext.callWithTenantId(
            workflowExecutionId.getTenantId(), () -> eligibility(workflowExecutionId, webhookId));

        if (eligibility == Eligibility.NOT_VOICE) {
            metricsRecorder.recordTokenRejected();

            throw new InvalidWebhookException(
                "Webhook is not a browser-voice trigger or does not exist: " + webhookId);
        }

        if (eligibility == Eligibility.DISABLED) {
            metricsRecorder.recordTokenRejected();

            throw new InvalidWebhookException("The workflow behind this webhook is disabled: " + webhookId);
        }

        String token = UUID.randomUUID()
            .toString();

        tokenToWebhookId.put(token, webhookId);

        metricsRecorder.recordTokenIssued();

        return new Token(token, TOKEN_TTL_SECONDS);
    }

    private Eligibility eligibility(WorkflowExecutionId workflowExecutionId, String webhookId) {
        try {
            JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
                workflowExecutionId.getType());

            String workflowId = jobPrincipalAccessor.getWorkflowId(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

            Workflow workflow = workflowService.getWorkflow(workflowId);

            List<WorkflowTrigger> triggers = WorkflowTrigger.of(workflow);

            WorkflowTrigger matchingTrigger = triggers.stream()
                .filter(trigger -> trigger.getName()
                    .equals(workflowExecutionId.getTriggerName()))
                .findFirst()
                .orElse(null);

            if (matchingTrigger == null || !VOICE_SESSION_TRIGGER_TYPE.equals(matchingTrigger.getType())) {
                return Eligibility.NOT_VOICE;
            }

            boolean enabled = jobPrincipalAccessor.isWorkflowEnabled(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

            return enabled ? Eligibility.VOICE : Eligibility.DISABLED;
        } catch (Exception exception) {
            if (log.isDebugEnabled()) {
                log.debug("Voice-capability check failed for webhookId={}: {}", webhookId, exception.getMessage());
            }

            return Eligibility.NOT_VOICE;
        }
    }

    private enum Eligibility {

        DISABLED, NOT_VOICE, VOICE
    }

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

    public static class VoiceUnavailableException extends RuntimeException {

        public VoiceUnavailableException(String message) {
            super(message);
        }
    }

    public static class InvalidWebhookException extends RuntimeException {

        public InvalidWebhookException(String message) {
            super(message);
        }
    }
}
