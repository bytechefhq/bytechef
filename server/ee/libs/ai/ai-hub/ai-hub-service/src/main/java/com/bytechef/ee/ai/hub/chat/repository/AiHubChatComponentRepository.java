/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChatComponent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Repository
public interface AiHubChatComponentRepository extends ListCrudRepository<AiHubChatComponent, Long> {

    List<AiHubChatComponent> findAllByChatId(long chatId);

    List<AiHubChatComponent> findAllByUserIdAndWorkspaceId(long userId, long workspaceId);

    Optional<AiHubChatComponent> findByUserIdAndWorkspaceIdAndComponentNameAndComponentVersion(
        long userId, long workspaceId, String componentName, int componentVersion);

    @Query("""
        SELECT * FROM ai_hub_chat_component
        WHERE chat_id = :chatId
        AND component_name = :componentName
        AND component_version = :componentVersion
        AND environment = :environment
        AND ((connection_id IS NULL AND :connectionId\\:\\:bigint IS NULL) OR connection_id = :connectionId)
        """)
    Optional<AiHubChatComponent> findIdempotencyMatch(
        long chatId, String componentName, int componentVersion, Long connectionId, int environment);

    @Query("""
        SELECT * FROM ai_hub_chat_component
        WHERE chat_id = :chatId
        AND component_name = :componentName
        AND component_version = :componentVersion
        AND environment = :environment
        LIMIT 1
        """)
    Optional<AiHubChatComponent> findByChatAndComponentIgnoringConnection(
        long chatId, String componentName, int componentVersion, int environment);
}
