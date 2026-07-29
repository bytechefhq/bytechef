/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.repository;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilitySessionRepository extends ListCrudRepository<AiObservabilitySession, Long> {

    /**
     * Sessions owned by one workspace. A session whose {@code workspace_id} is null belongs to no workspace and is
     * invisible here, which is the intended behavior for a workspace-scoped listing. Same for the two finders below.
     */
    List<AiObservabilitySession> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilitySession> findAllByWorkspaceIdAndUserId(Long workspaceId, String userId);

    Optional<AiObservabilitySession> findByWorkspaceIdAndExternalSessionId(Long workspaceId, String externalSessionId);
}
