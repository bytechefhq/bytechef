/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OwnerOnlyAccessPolicy implements AiHubChatAccessPolicy {

    @Override
    public boolean canView(AiHubChat chat, long userId) {
        return chat.getUserId() == userId;
    }

    @Override
    public boolean canParticipate(AiHubChat chat, long userId) {
        return chat.getUserId() == userId;
    }

    @Override
    public boolean canManage(AiHubChat chat, long userId) {
        return chat.getUserId() == userId;
    }
}
