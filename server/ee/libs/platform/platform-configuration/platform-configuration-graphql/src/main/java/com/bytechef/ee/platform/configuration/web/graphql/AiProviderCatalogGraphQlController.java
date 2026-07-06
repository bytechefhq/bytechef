/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderCatalogFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for reading the AI provider catalog.
 *
 * <p>
 * Authorization is enforced on {@link AiProviderCatalogFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AiProviderCatalogGraphQlController {

    private final AiProviderCatalogFacade aiProviderCatalogFacade;

    @SuppressFBWarnings("EI")
    AiProviderCatalogGraphQlController(AiProviderCatalogFacade aiProviderCatalogFacade) {
        this.aiProviderCatalogFacade = aiProviderCatalogFacade;
    }

    @QueryMapping
    public List<AiProviderCatalogItemDTO> aiProviderCatalog(@Argument Long environment) {
        return aiProviderCatalogFacade.getAiProviderCatalog(environment.intValue());
    }

    @QueryMapping
    public AiDefaultModelDTO aiDefaultModel(@Argument Long environment) {
        return aiProviderCatalogFacade.getAiDefaultModel(environment.intValue());
    }
}
