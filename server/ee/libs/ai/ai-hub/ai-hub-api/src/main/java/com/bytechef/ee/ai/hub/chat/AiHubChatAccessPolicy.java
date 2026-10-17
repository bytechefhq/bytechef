/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Decides who may reach an {@link AiHubChat}. The owner and a tenant admin may view, participate in, and manage a chat;
 * nobody else may.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatAccessPolicy {

    /**
     * Whether {@code userId} may read the chat's metadata and transcript.
     */
    boolean canView(AiHubChat chat, long userId);

    /**
     * Whether {@code userId} may contribute a turn to the chat: append a message or truncate history.
     */
    boolean canParticipate(AiHubChat chat, long userId);

    /**
     * Whether {@code userId} may change the chat's own settings or delete it.
     */
    boolean canManage(AiHubChat chat, long userId);
}
