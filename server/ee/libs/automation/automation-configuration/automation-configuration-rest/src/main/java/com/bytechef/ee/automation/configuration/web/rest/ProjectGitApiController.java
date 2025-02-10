/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.facade.ProjectGitFacade;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.web.rest.model.ProjectGitConfigurationModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ProjectGitApiController implements ProjectGitApi {

    private final ConversionService conversionService;
    private final ProjectGitFacade projectGitFacade;
    private final ProjectGitConfigurationService projectGitConfigurationService;

    @SuppressFBWarnings("EI")
    public ProjectGitApiController(
        ConversionService conversionService, ProjectGitFacade projectGitFacade,
        ProjectGitConfigurationService projectGitConfigurationService) {

        this.conversionService = conversionService;
        this.projectGitFacade = projectGitFacade;
        this.projectGitConfigurationService = projectGitConfigurationService;
    }

    @Override
    public ResponseEntity<ProjectGitConfigurationModel> getProjectGitConfiguration(Long id) {
        return projectGitConfigurationService.fetchProjectGitConfiguration(id)
            .map(projectGitConfiguration -> conversionService.convert(
                projectGitConfiguration, ProjectGitConfigurationModel.class))
            .map(ResponseEntity::ok)
            .orElse(noContent());
    }

    @Override
    public ResponseEntity<List<ProjectGitConfigurationModel>> getWorkspaceProjectGitConfigurations(Long id) {
        return ResponseEntity.ok(
            projectGitConfigurationService.getWorkspaceProjectGitConfigurations(id)
                .stream()
                .map(projectGitConfiguration -> conversionService.convert(
                    projectGitConfiguration, ProjectGitConfigurationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> pullProjectFromGit(Long id) {
        projectGitFacade.pullProjectFromGit(id);

        return noContent();
    }

    @Override
    public ResponseEntity<Void> updateProjectGitConfiguration(
        Long id, ProjectGitConfigurationModel projectGitConfigurationModel) {

        ProjectGitConfiguration projectGitConfiguration = conversionService.convert(
            projectGitConfigurationModel.projectId(id), ProjectGitConfiguration.class);

        projectGitConfigurationService.save(projectGitConfiguration);

        return noContent();
    }

    private static <T> ResponseEntity<T> noContent() {
        return ResponseEntity.noContent()
            .build();
    }
}
