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
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.web.rest.model.CheckWorkflowNodeTestOutputExists200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeTestOutputModel;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
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
public class WorkflowNodeTestOutputApiController implements WorkflowNodeTestOutputApi {

    private final ConversionService conversionService;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputApiController(
        ConversionService conversionService, WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.conversionService = conversionService;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public ResponseEntity<CheckWorkflowNodeTestOutputExists200ResponseModel> checkWorkflowNodeTestOutputExists(
        String id, String workflowNodeName, Long environmentId, OffsetDateTime createdDate) {

        // The service's hasPermission(#workflowId, 'Workflow', ...) gate is environment-agnostic, so the
        // caller-supplied environmentId is never checked. No @Cacheable/@WorkflowCacheEvict downstream, so
        // resolving here (rather than deeper in the service) is a matter of following the established pattern in
        // this controller, not a correctness requirement. See PrincipalEnvironment.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        return ResponseEntity.ok(
            new CheckWorkflowNodeTestOutputExists200ResponseModel().exists(
                workflowNodeTestOutputService.checkWorkflowNodeTestOutputExists(
                    id, workflowNodeName, createdDate == null ? null : createdDate.toInstant(),
                    effectiveEnvironmentId)));
    }

    @Override
    public ResponseEntity<Void> deleteWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, Long environmentId) {

        // Both the facade's and the service's own hasPermission(#workflowId, 'Workflow', ...) gates are
        // environment-agnostic, so the caller-supplied environmentId is never checked. Resolved HERE, at the
        // controller, rather than inside the gated methods below: deleteWorkflowNodeTestOutput and
        // saveWorkflowNodeTestOutput/saveWorkflowNodeSampleOutput's downstream save() are annotated
        // @WorkflowCacheEvict, whose aspect reads the environmentId argument via reflection off the ORIGINAL
        // call-site arguments (AspectJ's JoinPoint#getArgs()) -- a local variable reassigned inside the target
        // method is invisible to it. Resolving after entry would evict the requested environment's cache entry
        // while the method itself read or wrote the confined principal's own environment, leaving the two
        // permanently out of sync. See PrincipalEnvironment.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        workflowNodeTestOutputService.deleteWorkflowNodeTestOutput(
            workflowId, workflowNodeName, effectiveEnvironmentId);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    public ResponseEntity<WorkflowNodeTestOutputModel> saveWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, Long environmentId) {

        // See the comment on deleteWorkflowNodeTestOutput above -- same @WorkflowCacheEvict constraint applies to
        // the save() this facade method delegates to.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                    workflowId, workflowNodeName, effectiveEnvironmentId),
                WorkflowNodeTestOutputModel.class));
    }

    @Override
    public ResponseEntity<WorkflowNodeTestOutputModel> uploadWorkflowNodeSampleOutput(
        String workflowId, String workflowNodeName, Long environmentId, Object body) {

        // See the comment on deleteWorkflowNodeTestOutput above -- same @WorkflowCacheEvict constraint applies to
        // the save() this facade method delegates to.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeTestOutputFacade.saveWorkflowNodeSampleOutput(
                    workflowId, workflowNodeName, body, effectiveEnvironmentId),
                WorkflowNodeTestOutputModel.class));
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
