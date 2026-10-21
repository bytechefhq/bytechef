/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.WorkspaceConnectionApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppWorkspaceConnectionModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The workspace connection listing of the automation public API, for the {@code bytechef automation connection list}
 * command and the app loop. Lives with the app management routes so the CLI has one client from one spec.
 *
 * <p>
 * {@link AppFacade#getConnections} owns authorization, so this controller carries no {@code @PreAuthorize}; extending
 * {@link AbstractAppApiController} answers a denial with 403 instead of the global handler's 500.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.WorkspaceConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WorkspaceConnectionApiController extends AbstractAppApiController implements WorkspaceConnectionApi {

    private final AppFacade appFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public WorkspaceConnectionApiController(AppFacade appFacade, EnvironmentService environmentService) {
        this.appFacade = appFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<AppWorkspaceConnectionModel>> getWorkspaceConnections(
        Long workspaceId, @Nullable String componentName, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = environmentService.getEnvironment(
            xEnvironment == null ? null : xEnvironment.name());

        return ResponseEntity.ok(
            appFacade.getConnections(workspaceId, environment, componentName)
                .stream()
                .map(WorkspaceConnectionApiController::toModel)
                .toList());
    }

    private static AppWorkspaceConnectionModel toModel(AppWorkspaceConnectionDTO appWorkspaceConnectionDTO) {
        return new AppWorkspaceConnectionModel()
            .id(appWorkspaceConnectionDTO.id())
            .name(appWorkspaceConnectionDTO.name())
            .componentName(appWorkspaceConnectionDTO.componentName())
            .connectionVersion(appWorkspaceConnectionDTO.connectionVersion())
            .authorizationType(appWorkspaceConnectionDTO.authorizationType())
            .credentialStatus(appWorkspaceConnectionDTO.credentialStatus())
            .aiProvider(appWorkspaceConnectionDTO.aiProvider())
            .usable(appWorkspaceConnectionDTO.usable());
    }
}
