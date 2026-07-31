/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.guardrail.AiGatewayModerationClassifier;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Model-based moderation classifier: asks a configured moderation model to label the content {@code SAFE} or
 * {@code UNSAFE}. Registered only when {@code bytechef.ai.gateway.guardrails.moderation-model} names a model identifier
 * the gateway knows about; the model is resolved to its provider and called through the gateway's own chat-model
 * factory. Fails open (returns not-flagged) on any resolution or call error so a transient moderation outage does not
 * hard-block all gateway traffic.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway.guardrails", name = "moderation-model")
public class PromptBasedModerationClassifier implements AiGatewayModerationClassifier {

    private static final String CLASSIFIER_INSTRUCTION =
        "You are a strict content-safety classifier. Reply with exactly one word — SAFE or UNSAFE — indicating " +
            "whether the following content contains hate, harassment, sexual content involving minors, credible " +
            "violent threats, or self-harm instructions. Content:\n\n";

    private static final Logger log = LoggerFactory.getLogger(PromptBasedModerationClassifier.class);

    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    private final AiGatewayModelService aiGatewayModelService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final String moderationModel;

    @SuppressFBWarnings("EI")
    public PromptBasedModerationClassifier(
        AiGatewayChatModelFactory aiGatewayChatModelFactory, AiGatewayModelService aiGatewayModelService,
        AiGatewayProviderService aiGatewayProviderService,
        @Value("${bytechef.ai.gateway.guardrails.moderation-model:}") String moderationModel) {

        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
        this.aiGatewayModelService = aiGatewayModelService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.moderationModel = moderationModel;
    }

    @Override
    public boolean isFlagged(String content) {
        if (content == null || content.isBlank() || moderationModel.isBlank()) {
            return false;
        }

        try {
            Optional<AiGatewayModel> modelOptional = aiGatewayModelService.findByModelIdentifier(moderationModel);

            if (modelOptional.isEmpty()) {
                return false;
            }

            AiGatewayModel model = modelOptional.get();

            AiGatewayProvider provider = aiGatewayProviderService.getProvider(model.getProviderId());

            ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

            String verdict = chatModel.call(CLASSIFIER_INSTRUCTION + content);

            return verdict != null && verdict.toUpperCase(Locale.ROOT)
                .contains("UNSAFE");
        } catch (Exception exception) {
            log.warn("Moderation classification failed; failing open (not blocked): {}", exception.getMessage());

            return false;
        }
    }
}
