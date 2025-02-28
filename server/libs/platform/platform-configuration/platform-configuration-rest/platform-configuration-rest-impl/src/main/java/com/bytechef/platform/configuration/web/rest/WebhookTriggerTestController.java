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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.web.file.storage.TempFilesFileStorage;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
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

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    public WebhookTriggerTestController(
        TriggerDefinitionService triggerDefinitionService, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        WebhookTriggerTestFacade webhookTriggerTestFacade, WorkflowService workflowService, WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade) {

        super(new TempFilesFileStorage(), jobPrincipalAccessorRegistry, triggerDefinitionService, workflowService);

        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
    }

    @RequestMapping(
        method = {
            RequestMethod.HEAD, RequestMethod.GET, RequestMethod.POST
        },
        value = "/webhooks/{id}/test")
    public ResponseEntity<?> executeWorkflow(@PathVariable String id, HttpServletRequest httpServletRequest)
        throws Exception {

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);

        try {
            WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

            //workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput()

            return ResponseEntity.noContent()
                .build();
        } finally {
            webhookTriggerTestFacade.stopByWorkflowReferenceCode(
                workflowExecutionId.getWorkflowReferenceCode(), workflowExecutionId.getType());
        }
    }
}
