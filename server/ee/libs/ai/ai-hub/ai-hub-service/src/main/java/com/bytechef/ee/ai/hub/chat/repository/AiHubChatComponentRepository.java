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
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubChatComponentRepository extends ListCrudRepository<AiHubChatComponent, Long> {

    List<AiHubChatComponent> findAllByChatId(long chatId);

    /**
     * User-global "added connectors" (chat_id NULL, scoped by user + workspace). Powers the Connectors page list and
     * the resolver's union of user-global tools with the chat's own.
     */
    List<AiHubChatComponent> findAllByUserIdAndWorkspaceId(long userId, long workspaceId);

    /**
     * Idempotency lookup for adding a user-global connector — at most one row per (user, workspace, component, version)
     * per the partial unique index {@code uk_ahtc_user_comp}.
     */
    Optional<AiHubChatComponent> findByUserIdAndWorkspaceIdAndComponentNameAndComponentVersion(
        long userId, long workspaceId, String componentName, int componentVersion);

    /**
     * Idempotency lookup for the {@code attachComponent} flow. Mirrors the unique constraint
     * {@code uk_ccc_conv_comp_conn_env}: at most one row per (chat, component, version, connection, environment). Used
     * to make repeated chat-driven attachments converge on the existing row.
     */
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

    /**
     * Find any binding for the (chat, component, version, environment) tuple, IGNORING the connection. The autonomous
     * attach flow may issue {@code attachComponent(connectionId=null)} during discovery and then
     * {@code attachComponent(connectionId=42)} after the user picks one; without this lookup that sequence would create
     * two rows. The callback uses this to detect the "rebind connection" case and route to
     * {@link com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade#setComponentConnection} instead of a second insert.
     *
     * <p>
     * Returns the first matching binding when more than one exists — historically a chat-driven flow may have
     * accumulated multiple (null, real) pairs before this collapse was implemented; existing rows are not proactively
     * merged here.
     * </p>
     */
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
