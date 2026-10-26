/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.session;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailTokenSession;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailTokenSessionRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.ai.sensitivedata.PiiTokenSessionStore;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class EncryptedPiiTokenSessionStore implements PiiTokenSessionStore {

    private static final Logger log = LoggerFactory.getLogger(EncryptedPiiTokenSessionStore.class);

    private final AiGuardrailTokenSessionRepository aiGuardrailTokenSessionRepository;
    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    public EncryptedPiiTokenSessionStore(
        AiGuardrailTokenSessionRepository aiGuardrailTokenSessionRepository, Encryption encryption,
        ObjectMapper objectMapper) {

        this.aiGuardrailTokenSessionRepository = aiGuardrailTokenSessionRepository;
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoadResult load(SessionKey key) {
        try {
            Optional<AiGuardrailTokenSession> aiGuardrailTokenSession = aiGuardrailTokenSessionRepository
                .findByWorkspaceIdAndUserIdAndConversationId(key.workspaceId(), key.userId(), key.conversationId());

            if (aiGuardrailTokenSession.isEmpty()) {
                return LoadResult.of(Map.of());
            }

            Map<String, String> tokens = readTokens(aiGuardrailTokenSession.get());

            if (tokens == null) {
                return LoadResult.unavailable();
            }

            return LoadResult.of(tokens);
        } catch (Exception exception) {
            log.warn("Could not load the token session for conversation {}; continuing request-scoped",
                key.conversationId(), exception);

            return LoadResult.unavailable();
        }
    }

    @Override
    public void save(SessionKey key, Map<String, String> tokens) {
        try {
            if (tokens.isEmpty()) {
                aiGuardrailTokenSessionRepository
                    .findByWorkspaceIdAndUserIdAndConversationId(
                        key.workspaceId(), key.userId(), key.conversationId())
                    .ifPresent(aiGuardrailTokenSessionRepository::delete);

                return;
            }

            String sessionId = sessionIdOf(tokens);
            String encryptedTokens = encryption.encrypt(objectMapper.writeValueAsString(tokens));

            AiGuardrailTokenSession aiGuardrailTokenSession = aiGuardrailTokenSessionRepository
                .findByWorkspaceIdAndUserIdAndConversationId(key.workspaceId(), key.userId(), key.conversationId())
                .orElseGet(() -> new AiGuardrailTokenSession(
                    key.workspaceId(), key.userId(), key.conversationId(), sessionId, encryptedTokens));

            aiGuardrailTokenSession.setSessionId(sessionId);
            aiGuardrailTokenSession.setTokens(encryptedTokens);

            aiGuardrailTokenSessionRepository.save(aiGuardrailTokenSession);
        } catch (Exception exception) {
            log.warn("Could not save the token session for conversation {}; continuing request-scoped",
                key.conversationId(), exception);
        }
    }

    @Override
    public void evict(long workspaceId, String conversationId) {
        try {
            aiGuardrailTokenSessionRepository.deleteByWorkspaceIdAndConversationId(workspaceId, conversationId);
        } catch (Exception exception) {
            log.warn("Could not evict the token session for conversation {}", conversationId, exception);
        }
    }

    private @Nullable Map<String, String> readTokens(AiGuardrailTokenSession aiGuardrailTokenSession) {
        try {
            String decrypted = encryption.decrypt(aiGuardrailTokenSession.getTokens());

            return objectMapper.readValue(decrypted, new TypeReference<>() {});
        } catch (Exception exception) {
            log.warn("Could not decrypt the token session for conversation {}; continuing request-scoped",
                aiGuardrailTokenSession.getConversationId(), exception);

            return null;
        }
    }

    private String sessionIdOf(Map<String, String> tokens) {
        return PiiToken.sessionIdOf(tokens)
            .orElseThrow(
                () -> new IllegalArgumentException("No parseable PII token found to derive a session id from"));
    }
}
