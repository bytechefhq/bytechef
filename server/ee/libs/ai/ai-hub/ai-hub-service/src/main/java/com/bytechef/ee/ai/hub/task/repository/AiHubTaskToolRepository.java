/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task.repository;

import com.bytechef.ee.ai.hub.task.AiHubTaskTool;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubTaskToolRepository
    extends ListCrudRepository<AiHubTaskTool, Long> {

    List<AiHubTaskTool> findAllByTaskComponentId(long taskComponentId);

    Optional<AiHubTaskTool> findByTaskComponentIdAndName(long taskComponentId, String name);
}
