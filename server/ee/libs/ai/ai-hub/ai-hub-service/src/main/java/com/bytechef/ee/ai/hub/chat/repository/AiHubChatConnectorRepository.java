/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChatConnector;
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
public interface AiHubChatConnectorRepository extends ListCrudRepository<AiHubChatConnector, Long> {

    List<AiHubChatConnector> findAllByChatId(long chatId);

    /**
     * Upsert lookup for the composer's participation switch — at most one row per (chat, component) per the unique
     * constraint {@code uk_chc_chat_component}.
     */
    Optional<AiHubChatConnector> findByChatIdAndComponentName(long chatId, String componentName);
}
