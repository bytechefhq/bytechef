/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class IntegrationCodeWorkflowGraphQlController {

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public IntegrationCodeWorkflowGraphQlController(IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade) {
        this.integrationCodeWorkflowFacade = integrationCodeWorkflowFacade;
    }

    @QueryMapping
    public String integrationCodeWorkflowSource(@Argument long integrationId) {
        return integrationCodeWorkflowFacade.getCodeWorkflowSource(integrationId);
    }

    @MutationMapping
    public boolean updateIntegrationCodeWorkflowSource(@Argument long integrationId, @Argument String content) {
        integrationCodeWorkflowFacade.updateCodeWorkflowSource(integrationId, content);

        return true;
    }

    @MutationMapping
    public String createIntegrationCodeWorkflow(@Argument String componentName, @Argument Language language) {
        Integration integration = integrationCodeWorkflowFacade.createEmptyCodeWorkflow(componentName, language);

        return String.valueOf(integration.getId());
    }
}
