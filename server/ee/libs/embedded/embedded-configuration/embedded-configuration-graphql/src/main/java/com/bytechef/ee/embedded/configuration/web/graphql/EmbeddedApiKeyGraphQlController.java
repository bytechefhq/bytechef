/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ApiKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class EmbeddedApiKeyGraphQlController {

    private final EmbeddedApiKeyFacade embeddedApiKeyFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyGraphQlController(
        EmbeddedApiKeyFacade embeddedApiKeyFacade, EnvironmentService environmentService) {

        this.embeddedApiKeyFacade = embeddedApiKeyFacade;
        this.environmentService = environmentService;
    }

    @MutationMapping
    public String createEmbeddedApiKey(@Argument CreateEmbeddedApiKeyInput input) {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(environmentService.getEnvironment(input.environmentId()));
        apiKey.setName(input.name());

        return embeddedApiKeyFacade.createApiKey(apiKey);
    }

    @MutationMapping
    public boolean deleteEmbeddedApiKey(@Argument long id) {
        embeddedApiKeyFacade.deleteApiKey(id);

        return true;
    }

    @QueryMapping
    public ApiKey embeddedApiKey(@Argument long id) {
        return embeddedApiKeyFacade.getApiKey(id);
    }

    @QueryMapping
    public List<ApiKey> embeddedApiKeys(@Argument long environmentId) {
        return embeddedApiKeyFacade.getApiKeys(environmentId);
    }

    @MutationMapping
    public ApiKey updateEmbeddedApiKey(@Argument UpdateEmbeddedApiKeyInput input) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(input.id());
        apiKey.setName(input.name());

        return embeddedApiKeyFacade.updateApiKey(apiKey);
    }

    public record CreateEmbeddedApiKeyInput(String name, long environmentId) {
    }

    public record UpdateEmbeddedApiKeyInput(long id, String name) {
    }
}
