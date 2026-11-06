/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt.repository;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiPromptRepository extends ListCrudRepository<AiPrompt, Long> {

    List<AiPrompt> findAllByWorkspaceId(Long workspaceId);

    @Query("""
        SELECT ai_prompt.*
        FROM ai_prompt
        WHERE ai_prompt.workspace_id = :workspaceId
            AND (
                (ai_prompt.project_id IS NULL AND :projectId IS NULL)
                OR ai_prompt.project_id = :projectId
            )
            AND ai_prompt.name = :name
        """)
    Optional<AiPrompt> findByWorkspaceIdAndProjectIdAndName(
        @Param("workspaceId") Long workspaceId, @Param("projectId") Long projectId, @Param("name") String name);
}
