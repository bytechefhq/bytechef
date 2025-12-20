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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.web.rest.file.storage.TempFileStorageImpl;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class WebhookTriggerTestController extends AbstractWebhookTriggerController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookTriggerTestController.class);

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    public WebhookTriggerTestController(
        TriggerDefinitionService triggerDefinitionService, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        WebhookTriggerTestFacade webhookTriggerTestFacade, WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade,
        WorkflowService workflowService) {

        super(jobPrincipalAccessorRegistry, new TempFileStorageImpl(), triggerDefinitionService, workflowService);

        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
    }

    @RequestMapping(
        method = {
            RequestMethod.HEAD, RequestMethod.GET, RequestMethod.POST
        },
        value = "/webhooks/{id}/test/environments/{environmentId}")
    public ResponseEntity<?> executeWorkflow(
        @PathVariable String id, @PathVariable long environmentId, HttpServletRequest httpServletRequest) {

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        return TenantContext.callWithTenantId(workflowExecutionId.getTenantId(), () -> {
            ResponseEntity<?> responseEntity;
            WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);

            WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "executeWorkflow: id={}, webhookRequest={}, webhookTriggerFlags={}", workflowExecutionId,
                    webhookRequest, webhookTriggerFlags);
            }

            if (Objects.equals(httpServletRequest.getMethod(), RequestMethod.HEAD.name()) ||
                isWorkflowDisabled(workflowExecutionId)) {

                if (webhookTriggerFlags.workflowSyncOnEnableValidation()) {
                    responseEntity = doValidateOnEnable(workflowExecutionId, webhookRequest, environmentId);
                } else {
                    responseEntity = ResponseEntity.ok()
                        .build();
                }
            } else {
                workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                    workflowExecutionId, environmentId, webhookRequest);

                responseEntity = ResponseEntity.ok()
                    .build();
            }

            return responseEntity;
        });
    }

    @Override
    protected boolean isWorkflowDisabled(WorkflowExecutionId workflowExecutionId) {
        return !webhookTriggerTestFacade.isWorkflowEnabled(workflowExecutionId);
    }

    private ResponseEntity<?> doValidateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, long environmentId) {

        WebhookValidateResponse response = webhookTriggerTestFacade.validateOnEnable(
            workflowExecutionId, webhookRequest, environmentId);

        return ResponseEntity.status(response.status())
            .headers(
                response.headers() == null
                    ? null
                    : HttpHeaders.readOnlyHttpHeaders(new MultiValueMapAdapter<>(response.headers())))
            .body(response.body());
    }
}
