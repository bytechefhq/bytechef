/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.automation.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.configuration.public_.web.rest.ProjectApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ProjectCodeWorkflowApiController implements ProjectCodeWorkflowApi {

    private final EnvironmentService environmentService;
    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowApiController(
        EnvironmentService environmentService, ProjectCodeWorkflowFacade projectCodeWorkflowFacade) {

        this.environmentService = environmentService;
        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
    }

    @Override
    public ResponseEntity<Void> deployProject(
        Long workspaceId, EnvironmentModel xEnvironment, Boolean enable, MultipartFile projectFile) {

        try {
            projectCodeWorkflowFacade.save(
                workspaceId, projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())), getEnvironment(xEnvironment),
                enable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
