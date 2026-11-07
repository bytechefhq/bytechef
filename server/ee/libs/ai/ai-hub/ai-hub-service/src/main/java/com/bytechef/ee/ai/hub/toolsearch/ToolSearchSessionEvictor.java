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
import org.jspecify.annotations.Nullable;
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
    private final Supplier<@Nullable ToolSearchCatalogFeeder> toolSearchCatalogFeederSupplier;

    ToolSearchSessionEvictor(Supplier<List<ToolSearchToolCallingAdvisor>> toolSearchToolCallingAdvisorsSupplier) {
        this(toolSearchToolCallingAdvisorsSupplier, () -> null);
    }

    ToolSearchSessionEvictor(
        Supplier<List<ToolSearchToolCallingAdvisor>> toolSearchToolCallingAdvisorsSupplier,
        Supplier<@Nullable ToolSearchCatalogFeeder> toolSearchCatalogFeederSupplier) {

        this.toolSearchToolCallingAdvisorsSupplier = toolSearchToolCallingAdvisorsSupplier;
        this.toolSearchCatalogFeederSupplier = toolSearchCatalogFeederSupplier;
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

        ToolSearchCatalogFeeder toolSearchCatalogFeeder = toolSearchCatalogFeederSupplier.get();

        if (toolSearchCatalogFeeder == null) {
            return;
        }

        try {
            toolSearchCatalogFeeder.clearUserSessions(event.threadId());
        } catch (RuntimeException exception) {
            log.warn("Failed to clear the chat tool search sessions of thread {}", event.threadId(), exception);
        }
    }
}
