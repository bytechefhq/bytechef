/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.knowledgebase.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.knowledgebase.facade.EmbeddedKnowledgeBaseApiFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * The embedded console's knowledge base ownership surface. Authorization lives on
 * {@link EmbeddedKnowledgeBaseApiFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class EmbeddedKnowledgeBaseGraphQlController {

    private final EmbeddedKnowledgeBaseApiFacade embeddedKnowledgeBaseApiFacade;
    private final EnvironmentService environmentService;

    public EmbeddedKnowledgeBaseGraphQlController(
        EmbeddedKnowledgeBaseApiFacade embeddedKnowledgeBaseApiFacade, EnvironmentService environmentService) {

        this.embeddedKnowledgeBaseApiFacade = embeddedKnowledgeBaseApiFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<EmbeddedKnowledgeBase> embeddedKnowledgeBases(
        @Argument Long environmentId, @Argument @Nullable Long ownerId) {

        Environment environment = environmentService.getEnvironment(environmentId);

        List<KnowledgeBase> knowledgeBases = embeddedKnowledgeBaseApiFacade.getKnowledgeBases(
            environment.ordinal(), ownerId);

        return knowledgeBases.stream()
            .map(
                knowledgeBase -> new EmbeddedKnowledgeBase(
                    knowledgeBase.getId(), knowledgeBase.getName(), knowledgeBase.getDescription(),
                    toEpochMilli(knowledgeBase.getCreatedDate()), toEpochMilli(knowledgeBase.getLastModifiedDate()),
                    knowledgeBase.getOwnerId()))
            .toList();
    }

    @MutationMapping
    public boolean assignEmbeddedKnowledgeBaseOwner(@Argument AssignKnowledgeBaseOwnerInput input) {
        embeddedKnowledgeBaseApiFacade.assignKnowledgeBaseOwner(input.knowledgeBaseId(), input.ownerId());

        return true;
    }

    public record AssignKnowledgeBaseOwnerInput(Long knowledgeBaseId, @Nullable Long ownerId) {
    }

    private static @Nullable Long toEpochMilli(@Nullable Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    /**
     * Deliberately the same shape as the automation {@code KnowledgeBase} minus its documents, plus the owner, so one
     * set of client components can render both surfaces.
     */
    public record EmbeddedKnowledgeBase(Long id, String name, String description, @Nullable Long createdDate,
        @Nullable Long lastModifiedDate, @Nullable Long ownerId) {
    }
}
