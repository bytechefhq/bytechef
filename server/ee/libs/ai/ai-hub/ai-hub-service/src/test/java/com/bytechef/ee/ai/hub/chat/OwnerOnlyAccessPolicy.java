/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Test fixture standing in for {@link AiHubChatAccessPolicy} wherever a test only cares about owner semantics — every
 * predicate is true for the chat's own {@code userId} and false for anyone else. Deliberately not a Mockito mock: the
 * hand-built constructor tests this backs ({@code AiHubChatServiceTest}, {@code AiHubAgentConversationRecorderTest} and
 * its two integration siblings, plus {@code AiHubToolApprovalFacadeTest} in the sibling {@code approval} package)
 * exercise real owner-vs-stranger behaviour rather than asserting against whatever a mock was told to answer. Public so
 * it is reachable from that sibling package.
 *
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
