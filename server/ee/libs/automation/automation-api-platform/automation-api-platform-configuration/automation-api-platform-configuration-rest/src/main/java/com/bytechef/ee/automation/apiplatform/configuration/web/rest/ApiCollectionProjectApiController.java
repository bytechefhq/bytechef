/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/api-platform/internal")
@ConditionalOnCoordinator
public class ApiCollectionProjectApiController implements ProjectApi {

    private final ApiCollectionService apiCollectionService;
    private final ProjectService projectService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiCollectionProjectApiController(
        ApiCollectionService apiCollectionService, ProjectService projectService, ConversionService conversionService) {

        this.apiCollectionService = apiCollectionService;
        this.projectService = projectService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<ProjectBasicModel>> getWorkspaceApiCollectionProjects(Long id) {
        return ResponseEntity.ok(
            projectService.getProjects(apiCollectionService.getApiCollectionProjectIds(id))
                .stream()
                .map(project -> conversionService.convert(project, ProjectBasicModel.class))
                .toList());
    }
}
