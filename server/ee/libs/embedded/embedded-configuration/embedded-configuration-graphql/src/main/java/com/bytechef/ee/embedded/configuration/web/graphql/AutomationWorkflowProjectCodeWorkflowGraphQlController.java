/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowSourceDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
public class AutomationWorkflowProjectCodeWorkflowGraphQlController {

    private final AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectCodeWorkflowGraphQlController(
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade) {

        this.automationWorkflowProjectCodeWorkflowFacade = automationWorkflowProjectCodeWorkflowFacade;
    }

    @QueryMapping
    public AutomationWorkflowProjectCodeWorkflowSourceDTO automationWorkflowProjectCodeWorkflowSource(
        @Argument long automationWorkflowProjectId) {

        return automationWorkflowProjectCodeWorkflowFacade.getCodeWorkflowSource(automationWorkflowProjectId);
    }

    @MutationMapping
    public String createAutomationWorkflowProjectCodeWorkflow(
        @Argument String name, @Argument Language language, @Argument String description, @Argument String category,
        @Argument List<String> tags, @Argument String permissionExpression) {

        return String.valueOf(
            automationWorkflowProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
                name, language, description, category, tags, permissionExpression));
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProjectCodeWorkflowSource(
        @Argument long automationWorkflowProjectId, @Argument String content) {

        automationWorkflowProjectCodeWorkflowFacade.updateCodeWorkflowSource(automationWorkflowProjectId, content);

        return true;
    }
}
