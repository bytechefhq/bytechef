/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
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
public class CodeWorkflowGraphQlController {

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public CodeWorkflowGraphQlController(ProjectCodeWorkflowFacade projectCodeWorkflowFacade) {
        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
    }

    @QueryMapping
    public String codeWorkflowSource(@Argument long projectId) {
        return projectCodeWorkflowFacade.getCodeWorkflowSource(projectId);
    }

    @MutationMapping
    public boolean updateCodeWorkflowSource(@Argument long projectId, @Argument String content) {
        projectCodeWorkflowFacade.updateCodeWorkflowSource(projectId, content);

        return true;
    }

    @MutationMapping
    public String createCodeWorkflow(
        @Argument long workspaceId, @Argument String name, @Argument Language language) {

        Project project = projectCodeWorkflowFacade.createEmptyCodeWorkflow(workspaceId, name, language);

        return String.valueOf(project.getId());
    }
}
