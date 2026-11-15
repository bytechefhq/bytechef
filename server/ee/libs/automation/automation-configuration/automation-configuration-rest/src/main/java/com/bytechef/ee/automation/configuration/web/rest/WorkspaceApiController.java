/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.web.rest.model.WorkspaceModel;
import com.bytechef.ee.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authorization (ROLE_ADMIN) is enforced on {@link WorkspaceFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.configuration.web.rest.WorkspaceApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class WorkspaceApiController implements WorkspaceApi {

    private final WorkspaceFacade workspaceFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public WorkspaceApiController(WorkspaceFacade workspaceFacade, ConversionService conversionService) {
        this.workspaceFacade = workspaceFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<WorkspaceModel> createWorkspace(WorkspaceModel workspaceModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workspaceFacade.createWorkspace(conversionService.convert(workspaceModel, Workspace.class)),
                WorkspaceModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteWorkspace(Long id) {
        workspaceFacade.deleteWorkspace(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<WorkspaceModel> getWorkspace(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(workspaceFacade.getWorkspace(id), WorkspaceModel.class));
    }

    @Override
    public ResponseEntity<List<WorkspaceModel>> getWorkspaces() {
        return ResponseEntity.ok(
            workspaceFacade.getWorkspaces()
                .stream()
                .map(workspace -> conversionService.convert(workspace, WorkspaceModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<WorkspaceModel> updateWorkspace(Long id, WorkspaceModel workspaceModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workspaceFacade.updateWorkspace(conversionService.convert(workspaceModel.id(id), Workspace.class)),
                WorkspaceModel.class));
    }
}
