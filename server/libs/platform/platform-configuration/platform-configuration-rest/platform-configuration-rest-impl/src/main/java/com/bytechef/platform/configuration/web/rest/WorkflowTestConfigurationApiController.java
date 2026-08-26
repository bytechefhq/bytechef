/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.web.rest.model.DeleteWorkflowTestConfigurationConnectionRequestModel;
import com.bytechef.platform.configuration.web.rest.model.SaveWorkflowTestConfigurationInputsRequestModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationConnectionModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationModel;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class WorkflowTestConfigurationApiController implements WorkflowTestConfigurationApi {

    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public WorkflowTestConfigurationApiController(
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade, ConversionService conversionService) {

        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> getWorkflowTestConfiguration(
        String workflowId, Long environmentId) {

        // Previously called WorkflowTestConfigurationService directly, which carries no @PreAuthorize -- any
        // authenticated caller could read any workflow's test configuration. Routed through the facade instead, so
        // a gate exists at all; the facade's hasPermission(#workflowId, 'Workflow', ...) is itself
        // environment-agnostic, so the caller-supplied environmentId is resolved here first. See PrincipalEnvironment.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationFacade.fetchWorkflowTestConfiguration(workflowId, effectiveEnvironmentId)
                    .orElse(null),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowTestConfigurationConnectionModel>> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName, Long environmentId) {

        // Same gap as getWorkflowTestConfiguration above: routed through the facade so a gate exists, environmentId
        // resolved here first. See PrincipalEnvironment.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowTestConfigurationFacade.getWorkflowTestConfigurationConnections(
                    workflowId, workflowNodeName, effectiveEnvironmentId),
                workflowTestConfigurationConnection -> conversionService.convert(
                    workflowTestConfigurationConnection, WorkflowTestConfigurationConnectionModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> saveWorkflowTestConfiguration(
        String workflowId, WorkflowTestConfigurationModel workflowTestConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
                    conversionService.convert(
                        workflowTestConfigurationModel.workflowId(workflowId), WorkflowTestConfiguration.class)),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String workflowConnectionKey, Long environmentId,
        DeleteWorkflowTestConfigurationConnectionRequestModel deleteWorkflowTestConfigurationConnectionRequestModel) {

        workflowTestConfigurationFacade.deleteWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey,
            deleteWorkflowTestConfigurationConnectionRequestModel.getConnectionId(), environmentId);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> saveWorkflowTestConfigurationInputs(
        String workflowId, Long environmentId,
        SaveWorkflowTestConfigurationInputsRequestModel saveWorkflowTestConfigurationInputsRequestModel) {

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationInputs(
            workflowId, saveWorkflowTestConfigurationInputsRequestModel.getKey(),
            saveWorkflowTestConfigurationInputsRequestModel.getValue()
                .orElse(null),
            environmentId);

        return ResponseEntity.noContent()
            .build();
    }

    // Required by the OpenAPI contract (@NotNull, required = true) on every caller of this method, so Spring
    // rejects a missing environmentId before any of them run -- checked explicitly all the same, because the
    // alternative is an unboxing NPE surfacing as a 500 if that ever changes.
    private static long resolveRequiredEnvironmentId(Long environmentId) {
        if (environmentId == null) {
            throw new IllegalArgumentException("environmentId is required");
        }

        return PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId.longValue());
    }
}
