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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.web.rest.model.WorkspaceModel;
import com.bytechef.edition.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCEVersion
@ConditionalOnEndpoint
public class WorkspaceApiController implements WorkspaceApi {

    private final ConversionService conversionService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public WorkspaceApiController(ConversionService conversionService, WorkspaceFacade workspaceFacade) {
        this.conversionService = conversionService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    public ResponseEntity<List<WorkspaceModel>> getUserWorkspaces(Long id) {
        return ResponseEntity.ok(
            workspaceFacade.getUserWorkspaces(id)
                .stream()
                .map(workspace -> conversionService.convert(workspace, WorkspaceModel.class))
                .toList());
    }
}
