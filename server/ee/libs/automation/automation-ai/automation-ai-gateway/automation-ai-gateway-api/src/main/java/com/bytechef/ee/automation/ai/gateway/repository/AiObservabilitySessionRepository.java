/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilitySessionRepository extends ListCrudRepository<AiObservabilitySession, Long> {

    List<AiObservabilitySession> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilitySession> findAllByWorkspaceIdAndUserId(Long workspaceId, String userId);

    Optional<AiObservabilitySession> findByWorkspaceIdAndExternalSessionId(Long workspaceId, String externalSessionId);
}
