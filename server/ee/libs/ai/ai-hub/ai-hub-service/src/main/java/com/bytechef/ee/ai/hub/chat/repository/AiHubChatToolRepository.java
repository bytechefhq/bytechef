/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChatTool;
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
public interface AiHubChatToolRepository
    extends ListCrudRepository<AiHubChatTool, Long> {

    List<AiHubChatTool> findAllByChatComponentId(long chatComponentId);

    Optional<AiHubChatTool> findByChatComponentIdAndName(long chatComponentId, String name);
}
