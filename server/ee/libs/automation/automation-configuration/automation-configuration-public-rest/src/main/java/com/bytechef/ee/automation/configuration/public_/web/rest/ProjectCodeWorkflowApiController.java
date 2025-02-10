/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
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

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowApiController(ProjectCodeWorkflowFacade projectCodeWorkflowFacade) {

        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
    }

    @Override
    public ResponseEntity<Void> deployProject(Long projectId, MultipartFile projectFile) {
        try {
            projectCodeWorkflowFacade.save(
                projectId, projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
