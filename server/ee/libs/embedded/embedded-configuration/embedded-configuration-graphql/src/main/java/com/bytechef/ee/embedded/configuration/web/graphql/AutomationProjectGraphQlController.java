/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCodeWorkflowStateDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectGraphQlController {

    private final AutomationProjectFacade automationProjectFacade;
    private final AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectGraphQlController(
        AutomationProjectFacade automationProjectFacade,
        AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade) {

        this.automationProjectFacade = automationProjectFacade;
        this.automationProjectCodeWorkflowFacade = automationProjectCodeWorkflowFacade;
    }

    @SchemaMapping(typeName = "AutomationProject")
    public Boolean codeWorkflowDraftPublishable(AutomationProjectDTO automationProjectDTO) {
        return fetchCodeWorkflowState(automationProjectDTO)
            .map(AutomationProjectCodeWorkflowStateDTO::draftPublishable)
            .orElse(null);
    }

    @SchemaMapping(typeName = "AutomationProject")
    public Language codeWorkflowLanguage(AutomationProjectDTO automationProjectDTO) {
        return fetchCodeWorkflowState(automationProjectDTO)
            .map(AutomationProjectCodeWorkflowStateDTO::language)
            .orElse(null);
    }

    @QueryMapping
    public List<AutomationProjectDTO> automationProjects(@Argument @Nullable String environmentId) {
        if (environmentId == null) {
            return automationProjectFacade.getProjects();
        }

        return automationProjectFacade.getProjects(Long.parseLong(environmentId));
    }

    @QueryMapping
    public List<AutomationProjectCategoryDTO> automationProjectCategories() {
        return automationProjectFacade.getCategories();
    }

    @QueryMapping
    public List<AutomationProjectTagDTO> automationProjectTags() {
        return automationProjectFacade.getTags();
    }

    @MutationMapping
    public String createAutomationProject(
        @Argument String name, @Argument String description, @Argument String category,
        @Argument List<String> tags, @Argument String permissionExpression) {

        return String.valueOf(
            automationProjectFacade.createProject(
                name, description, category, tags == null ? List.of() : tags, permissionExpression));
    }

    @MutationMapping
    public boolean updateAutomationProject(
        @Argument String id, @Argument String name, @Argument String description, @Argument String category,
        @Argument List<String> tags, @Argument String permissionExpression) {

        automationProjectFacade.updateProject(
            Long.parseLong(id), name, description, category, tags == null ? List.of() : tags, permissionExpression);

        return true;
    }

    @MutationMapping
    public boolean deleteAutomationProject(@Argument String id) {
        automationProjectFacade.deleteProject(Long.parseLong(id));

        return true;
    }

    @MutationMapping
    public boolean publishAutomationProject(@Argument String id, @Argument @Nullable String description) {
        automationProjectFacade.publishProject(Long.parseLong(id), description);

        return true;
    }

    @MutationMapping
    public String createAutomationProjectWorkflow(
        @Argument String projectId, @Argument String definition, @Argument String permissionExpression) {

        return automationProjectFacade.createProjectWorkflow(
            Long.parseLong(projectId), definition, permissionExpression);
    }

    @MutationMapping
    public boolean updateAutomationProjectWorkflow(
        @Argument String workflowUuid, @Argument String label, @Argument String description) {

        automationProjectFacade.updateProjectWorkflow(workflowUuid, label, description);

        return true;
    }

    @MutationMapping
    public boolean updateAutomationProjectWorkflowPermissionExpression(
        @Argument String workflowUuid, @Argument String permissionExpression) {

        automationProjectFacade.updateProjectWorkflowPermissionExpression(workflowUuid, permissionExpression);

        return true;
    }

    @MutationMapping
    public boolean deleteAutomationProjectWorkflow(@Argument String workflowUuid) {
        automationProjectFacade.deleteProjectWorkflow(workflowUuid);

        return true;
    }

    @QueryMapping
    public VersionWorkflowChange automationProjectVersionWorkflowChange(
        @Argument String id, @Argument int projectVersion, @Argument String workflowUuid) {

        return automationProjectFacade.getProjectVersionWorkflowChange(
            Long.parseLong(id), projectVersion, workflowUuid);
    }

    @QueryMapping
    public VersionWorkflowChanges automationProjectVersionWorkflowChanges(
        @Argument String id, @Argument int projectVersion) {

        return automationProjectFacade.getProjectVersionWorkflowChanges(Long.parseLong(id), projectVersion);
    }

    @QueryMapping
    public List<AutomationProjectVersionDTO> automationProjectVersions(@Argument String id) {
        return automationProjectFacade.getProjectVersions(Long.parseLong(id));
    }

    @MutationMapping
    public String duplicateAutomationProjectWorkflow(@Argument String workflowUuid) {
        return automationProjectFacade.duplicateProjectWorkflow(workflowUuid);
    }

    @MutationMapping
    public String duplicateAutomationProject(@Argument String id) {
        return String.valueOf(automationProjectFacade.duplicateProject(Long.parseLong(id)));
    }

    private Optional<AutomationProjectCodeWorkflowStateDTO> fetchCodeWorkflowState(
        AutomationProjectDTO automationProjectDTO) {

        if (!automationProjectDTO.codeWorkflowProject()) {
            return Optional.empty();
        }

        return automationProjectCodeWorkflowFacade.getCodeWorkflowState(automationProjectDTO.id());
    }
}
