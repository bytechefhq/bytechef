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

package com.bytechef.platform.webhook.web;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.tenant.util.TenantUtils;
import com.bytechef.platform.webhook.executor.WorkflowExecutor;
import com.bytechef.platform.webhook.web.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
public class WebhookTriggerController extends AbstractWebhookTriggerController {

    private final WorkflowExecutor workflowExecutor;

    @SuppressFBWarnings("EI")
    public WebhookTriggerController(
        ApplicationProperties applicationProperties, FilesFileStorage filesFileStorage,
        InstanceAccessorRegistry instanceAccessorRegistry, TriggerDefinitionService triggerDefinitionService,
        WorkflowExecutor workflowExecutor, WorkflowService workflowService) {

        super(
            filesFileStorage, instanceAccessorRegistry, applicationProperties.getPublicUrl(), triggerDefinitionService,
            workflowExecutor, workflowService);

        this.workflowExecutor = workflowExecutor;
    }

    @RequestMapping(
        method = {
            RequestMethod.HEAD, RequestMethod.GET, RequestMethod.POST
        },
        value = "/webhooks/{id}")
    public ResponseEntity<?> executeWorkflow(
        @PathVariable String id, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        return TenantUtils.callWithTenantId(
            workflowExecutionId.getTenantId(), () -> {
                ResponseEntity<?> responseEntity;

                if (Objects.equals(httpServletRequest.getMethod(), RequestMethod.HEAD.name()) ||
                    isWorkflowDisabled(workflowExecutionId)) {

                    WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);

                    WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

                    if (webhookTriggerFlags.workflowSyncOnEnableValidation()) {
                        responseEntity = doValidateOnEnable(workflowExecutionId, webhookRequest);
                    } else {
                        responseEntity = ResponseEntity.ok()
                            .build();
                    }
                } else {
                    responseEntity = doProcessTrigger(
                        workflowExecutionId, null, httpServletRequest, httpServletResponse);
                }

                return responseEntity;
            });
    }

    private ResponseEntity<?> doValidateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        WebhookValidateResponse response = workflowExecutor.validateOnEnable(
            workflowExecutionId, webhookRequest);

        return ResponseEntity.status(response.status())
            .headers(
                response.headers() == null
                    ? null
                    : HttpHeaders.readOnlyHttpHeaders(new MultiValueMapAdapter<>(response.headers())))
            .body(response.body());
    }
}
