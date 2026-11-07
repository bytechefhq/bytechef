/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubChatBindingCleanupListener {

    private final AiHubChatToolFacade chatToolFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatBindingCleanupListener(AiHubChatToolFacade chatToolFacade) {
        this.chatToolFacade = chatToolFacade;
    }

    @EventListener
    public void onChatDeleted(AiHubChatDeletedEvent event) {
        chatToolFacade.deleteChatBindings(event.chatId());
    }
}
