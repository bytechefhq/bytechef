/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
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
public class AutomationWorkflowProjectGraphQlController {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectGraphQlController(AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @QueryMapping
    public List<AutomationWorkflowProjectDTO> automationWorkflowProjects() {
        return automationWorkflowProjectFacade.getProjects();
    }

    @QueryMapping
    public List<AutomationWorkflowProjectCategoryDTO> automationWorkflowProjectCategories() {
        return automationWorkflowProjectFacade.getCategories();
    }

    @QueryMapping
    public List<AutomationWorkflowProjectTagDTO> automationWorkflowProjectTags() {
        return automationWorkflowProjectFacade.getTags();
    }

    @MutationMapping
    public String createAutomationWorkflowProject(
        @Argument String name, @Argument String description, @Argument String category,
        @Argument List<String> tags) {

        return String.valueOf(
            automationWorkflowProjectFacade.createProject(name, description, category,
                tags == null ? List.of() : tags));
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProject(
        @Argument String id, @Argument String name, @Argument String description, @Argument String category,
        @Argument List<String> tags) {

        automationWorkflowProjectFacade.updateProject(
            Long.parseLong(id), name, description, category, tags == null ? List.of() : tags);

        return true;
    }

    @MutationMapping
    public boolean deleteAutomationWorkflowProject(@Argument String id) {
        automationWorkflowProjectFacade.deleteProject(Long.parseLong(id));

        return true;
    }

    @MutationMapping
    public boolean publishAutomationWorkflowProject(@Argument String id) {
        automationWorkflowProjectFacade.publishProject(Long.parseLong(id));

        return true;
    }

    @MutationMapping
    public String createAutomationWorkflowProjectWorkflow(@Argument String projectId, @Argument String definition) {
        return automationWorkflowProjectFacade.createProjectWorkflow(Long.parseLong(projectId), definition);
    }

    @MutationMapping
    public boolean deleteAutomationWorkflowProjectWorkflow(@Argument String workflowUuid) {
        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);

        return true;
    }

    @QueryMapping
    public List<AutomationWorkflowProjectVersionDTO> automationWorkflowProjectVersions(@Argument String id) {
        return automationWorkflowProjectFacade.getProjectVersions(Long.parseLong(id));
    }

    @MutationMapping
    public String duplicateAutomationWorkflowProjectWorkflow(@Argument String workflowUuid) {
        return automationWorkflowProjectFacade.duplicateProjectWorkflow(workflowUuid);
    }

    @MutationMapping
    public String duplicateAutomationWorkflowProject(@Argument String id) {
        return String.valueOf(automationWorkflowProjectFacade.duplicateProject(Long.parseLong(id)));
    }
}
