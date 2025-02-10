/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.facade.ProjectGitFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.configuration.public_.web.rest.ProjectGitApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ProjectGitApiController implements ProjectGitApi {

    private final ProjectGitFacade projectGitFacade;

    @SuppressFBWarnings("EI")
    public ProjectGitApiController(ProjectGitFacade projectGitFacade) {
        this.projectGitFacade = projectGitFacade;
    }

    @Override
    public ResponseEntity<Void> pullProjectFromGit(Long id) {
        projectGitFacade.pullProjectFromGit(id);

        return ResponseEntity.noContent()
            .build();
    }
}
