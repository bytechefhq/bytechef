/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.automation.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.automation.configuration.web.rest.WorkspaceApi;
import com.bytechef.automation.configuration.web.rest.model.WorkspaceModel;
import com.bytechef.edition.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class WorkspaceApiController implements WorkspaceApi {

    private final ConversionService conversionService;
    private final WorkspaceFacade workspaceFacade;
    private final WorkspaceService workspaceService;

    @SuppressFBWarnings("EI")
    public WorkspaceApiController(
        ConversionService conversionService, WorkspaceFacade workspaceFacade, WorkspaceService workspaceService) {

        this.conversionService = conversionService;
        this.workspaceFacade = workspaceFacade;
        this.workspaceService = workspaceService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<WorkspaceModel> createWorkspace(WorkspaceModel workspaceModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workspaceService.create(conversionService.convert(workspaceModel, Workspace.class)),
                WorkspaceModel.class));
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteWorkspace(Long id) {
        workspaceService.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<WorkspaceModel>> getUserWorkspaces(Long id) {
        return ResponseEntity.ok(
            workspaceFacade.getUserWorkspaces(id)
                .stream()
                .map(workspace -> conversionService.convert(workspace, WorkspaceModel.class))
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<WorkspaceModel> getWorkspace(Long id) {
        return ResponseEntity.ok(conversionService.convert(workspaceService.getWorkspace(id), WorkspaceModel.class));
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<List<WorkspaceModel>> getWorkspaces() {
        return ResponseEntity.ok(
            workspaceService.getWorkspaces()
                .stream()
                .map(workspace -> conversionService.convert(workspace, WorkspaceModel.class))
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<WorkspaceModel> updateWorkspace(Long id, WorkspaceModel workspaceModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workspaceService.update(conversionService.convert(workspaceModel.id(id), Workspace.class)),
                WorkspaceModel.class));
    }
}
