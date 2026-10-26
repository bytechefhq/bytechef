/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.repository;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailTokenSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGuardrailTokenSessionRepository extends ListCrudRepository<AiGuardrailTokenSession, Long> {

    Optional<AiGuardrailTokenSession> findByWorkspaceIdAndUserIdAndConversationId(
        long workspaceId, long userId, String conversationId);

    List<AiGuardrailTokenSession> findByLastModifiedDateBefore(Instant lastModifiedDate);

    void deleteByWorkspaceIdAndConversationId(long workspaceId, String conversationId);
}
