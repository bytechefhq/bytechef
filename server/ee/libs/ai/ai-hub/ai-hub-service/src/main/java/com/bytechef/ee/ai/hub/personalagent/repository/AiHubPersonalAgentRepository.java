/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent.repository;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link AiHubPersonalAgent}. Sidebar lookups join through
 * {@code workspace_ai_hub_personal_agent} for the workspace dimension; this repository only sees the workspace-agnostic
 * entity surface (user_id + environment). The {@link #findAllByWorkspaceUserEnvironment} query is the single
 * workspace-aware exception — it does the JOIN inline so callers can fetch the agent rows ordered by
 * {@code updated_at DESC} in one round trip.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubPersonalAgentRepository extends CrudRepository<AiHubPersonalAgent, Long> {

    /**
     * Sidebar listing — all agents belonging to the (workspace, user, environment) triple, sorted by most-recently
     * updated first so freshly-touched agents float to the top of the list. Returns an empty list when the user has no
     * agents yet rather than throwing — sidebar render handles the empty state with a "Create your first agent" CTA,
     * not an error.
     */
    @Query("""
        SELECT cca.* FROM ai_hub_personal_agent cca
        JOIN workspace_ai_hub_personal_agent wcca
          ON wcca.ai_hub_personal_agent_id = cca.id
        WHERE wcca.workspace_id = :workspaceId
          AND cca.user_id = :userId
          AND cca.environment = :environment
        ORDER BY cca.updated_at DESC
        """)
    List<AiHubPersonalAgent> findAllByWorkspaceUserEnvironment(
        long workspaceId, long userId, int environment);

    /**
     * Resolve an agent by name within a (workspace, user, environment). Used by the LLM create-or-resolve tool flow:
     * names are not enforced unique at the DB layer, so multiple matches are theoretically possible — the service layer
     * guards against that by failing the create when a row already exists for the (workspace, user, env, name) tuple.
     * The JOIN through workspace_ai_hub_personal_agent keeps the lookup workspace-scoped.
     */
    @Query("""
        SELECT cca.* FROM ai_hub_personal_agent cca
        JOIN workspace_ai_hub_personal_agent wcca
          ON wcca.ai_hub_personal_agent_id = cca.id
        WHERE wcca.workspace_id = :workspaceId
          AND cca.user_id = :userId
          AND cca.environment = :environment
          AND cca.name = :name
        """)
    List<AiHubPersonalAgent> findAllByWorkspaceUserEnvironmentAndName(
        long workspaceId, long userId, int environment, String name);
}
