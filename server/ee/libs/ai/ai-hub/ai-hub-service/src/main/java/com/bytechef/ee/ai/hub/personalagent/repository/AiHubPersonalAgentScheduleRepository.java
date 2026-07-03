/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent.repository;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubPersonalAgentScheduleRepository extends CrudRepository<AiHubPersonalAgentSchedule, Long> {

    Optional<AiHubPersonalAgentSchedule> findByAiHubPersonalAgentId(long aiHubPersonalAgentId);

    List<AiHubPersonalAgentSchedule> findByWorkspaceIdAndUserId(long workspaceId, long userId);

    @Query("SELECT * FROM ai_hub_personal_agent_schedule WHERE enabled = true")
    List<AiHubPersonalAgentSchedule> findAllEnabled();
}
