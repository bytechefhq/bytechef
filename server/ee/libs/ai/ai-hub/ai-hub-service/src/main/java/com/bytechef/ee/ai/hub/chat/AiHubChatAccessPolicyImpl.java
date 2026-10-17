/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link AiHubChatAccessPolicy}: the chat's owner, or a user holding the tenant {@code ADMIN}
 * authority, may view, participate in and manage the chat.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubChatAccessPolicyImpl implements AiHubChatAccessPolicy {

    @Override
    public boolean canView(AiHubChat chat, long userId) {
        return isOwnerOrAdmin(chat, userId);
    }

    @Override
    public boolean canParticipate(AiHubChat chat, long userId) {
        return isOwnerOrAdmin(chat, userId);
    }

    @Override
    public boolean canManage(AiHubChat chat, long userId) {
        return isOwnerOrAdmin(chat, userId);
    }

    private static boolean isOwnerOrAdmin(AiHubChat chat, long userId) {
        return chat.getUserId() == userId || SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }
}
