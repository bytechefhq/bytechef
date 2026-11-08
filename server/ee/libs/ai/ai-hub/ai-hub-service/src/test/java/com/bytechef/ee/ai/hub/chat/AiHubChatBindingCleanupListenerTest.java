/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatBindingCleanupListenerTest {

    @Test
    void testDeletingAChatDeletesItsBindings() {
        AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);

        new AiHubChatBindingCleanupListener(chatToolFacade).onChatDeleted(new AiHubChatDeletedEvent(7L, "thread-7"));

        verify(chatToolFacade).deleteChatBindings(7L);
    }
}
