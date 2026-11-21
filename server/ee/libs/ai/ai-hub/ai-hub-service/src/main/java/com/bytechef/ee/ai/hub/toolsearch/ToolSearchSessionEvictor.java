/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ee.ai.hub.chat.AiHubChatDeletedEvent;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ToolSearchSessionEvictor {

    private static final Logger log = LoggerFactory.getLogger(ToolSearchSessionEvictor.class);

    private final Supplier<List<ToolSearchToolCallingAdvisor>> toolSearchToolCallingAdvisorsSupplier;

    ToolSearchSessionEvictor(Supplier<List<ToolSearchToolCallingAdvisor>> toolSearchToolCallingAdvisorsSupplier) {
        this.toolSearchToolCallingAdvisorsSupplier = toolSearchToolCallingAdvisorsSupplier;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChatDeleted(AiHubChatDeletedEvent event) {
        for (ToolSearchToolCallingAdvisor toolSearchToolCallingAdvisor : toolSearchToolCallingAdvisorsSupplier.get()) {
            try {
                toolSearchToolCallingAdvisor.evictSession(event.threadId());
            } catch (RuntimeException exception) {
                log.warn("Failed to clear the tool search session of thread {}", event.threadId(), exception);
            }
        }
    }
}
