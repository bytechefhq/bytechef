/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.repository;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface WorkspaceAiObservabilitySessionRepository
    extends ListCrudRepository<WorkspaceAiObservabilitySession, Long> {

    Optional<WorkspaceAiObservabilitySession> findByAiObservabilitySessionId(long aiObservabilitySessionId);

    @Query("""
        SELECT ai_observability_session.*
        FROM ai_observability_session
        JOIN workspace_ai_observability_session
            ON workspace_ai_observability_session.ai_observability_session_id = ai_observability_session.id
        WHERE workspace_ai_observability_session.workspace_id = :workspaceId
        """)
    List<AiObservabilitySession> findAllSessionsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_observability_session.*
        FROM ai_observability_session
        JOIN workspace_ai_observability_session
            ON workspace_ai_observability_session.ai_observability_session_id = ai_observability_session.id
        WHERE workspace_ai_observability_session.workspace_id = :workspaceId
          AND ai_observability_session.user_id = :userId
        """)
    List<AiObservabilitySession> findAllSessionsByWorkspaceIdAndUserId(
        @Param("workspaceId") Long workspaceId, @Param("userId") String userId);

    @Query("""
        SELECT ai_observability_session.*
        FROM ai_observability_session
        JOIN workspace_ai_observability_session
            ON workspace_ai_observability_session.ai_observability_session_id = ai_observability_session.id
        WHERE workspace_ai_observability_session.workspace_id = :workspaceId
          AND ai_observability_session.external_session_id = :externalSessionId
        """)
    Optional<AiObservabilitySession> findByWorkspaceIdAndExternalSessionId(
        @Param("workspaceId") Long workspaceId, @Param("externalSessionId") String externalSessionId);
}
