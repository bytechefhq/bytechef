/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent.repository;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubPersonalAgentResource}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubPersonalAgentResourceRepository extends CrudRepository<AiHubPersonalAgentResource, Long> {

    /**
     * Lists all resource template rows for an agent ordered by creation time so the form and the task copy path see the
     * resources in the order the user added them.
     */
    List<AiHubPersonalAgentResource> findByAiHubPersonalAgentIdOrderByCreatedAtAsc(long aiHubPersonalAgentId);

    /**
     * Lookup by the natural-key tuple. Used to make {@code addResource} idempotent — the unique constraint catches the
     * race; the pre-flight check lets the service return a typed "already added" rather than a 500 from the DB
     * exception. {@code kind} is the INT ordinal of {@code AiHubPersonalAgentResourceKind}.
     */
    Optional<AiHubPersonalAgentResource> findByAiHubPersonalAgentIdAndKindAndResourceId(
        long aiHubPersonalAgentId, int kind, String resourceId);
}
