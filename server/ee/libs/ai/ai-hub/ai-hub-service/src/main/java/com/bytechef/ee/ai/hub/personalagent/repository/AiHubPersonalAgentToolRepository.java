/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent.repository;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentTool;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubPersonalAgentTool}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubPersonalAgentToolRepository extends CrudRepository<AiHubPersonalAgentTool, Long> {

    /**
     * Lists all tool template rows for an agent ordered by creation time so the dialog and the task copy path see the
     * tools in the order the user added them. Order matters for UX consistency — the picker rendered in the order rows
     * were inserted, and the LLM tool list mirrors that order.
     */
    List<AiHubPersonalAgentTool>
        findByAiHubPersonalAgentIdOrderByCreatedAtAsc(long aiHubPersonalAgentId);

    /**
     * Lookup by the natural-key tuple. Used to make {@code addTool} idempotent — the unique constraint catches the race
     * condition but the pre-flight check lets us return a typed "already added" rather than a 500 from the DB
     * exception.
     */
    Optional<AiHubPersonalAgentTool>
        findByAiHubPersonalAgentIdAndComponentNameAndComponentVersionAndOperationName(
            long aiHubPersonalAgentId, String componentName, int componentVersion, String operationName);
}
