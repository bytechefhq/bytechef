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
import java.util.List;
import org.jspecify.annotations.Nullable;
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
                    knowledgeBase.getId(), knowledgeBase.getName(), knowledgeBase.getDescription()))
            .toList();
    }

    @MutationMapping
    public boolean assignEmbeddedKnowledgeBaseOwner(@Argument AssignKnowledgeBaseOwnerInput input) {
        embeddedKnowledgeBaseApiFacade.assignKnowledgeBaseOwner(input.knowledgeBaseId(), input.ownerId());

        return true;
    }

    public record AssignKnowledgeBaseOwnerInput(Long knowledgeBaseId, @Nullable Long ownerId) {
    }

    public record EmbeddedKnowledgeBase(Long id, String name, String description) {
    }
}
