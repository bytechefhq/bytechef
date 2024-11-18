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

package com.bytechef.embedded.execution.public_.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.embedded.execution.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.webhook.executor.WorkflowExecutor;
import com.bytechef.platform.webhook.web.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Ivica Cardic
 */
@Controller
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class RequestTriggerApiController extends AbstractWebhookTriggerController implements RequestTriggerApi {

    private final ConnectedUserService connectedUserService;
    private final HttpServletRequest httpServletRequest;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public RequestTriggerApiController(
        ConnectedUserService connectedUserService, FilesFileStorage filesFileStorage,
        HttpServletRequest httpServletRequest, InstanceAccessorRegistry instanceAccessorRegistry,
        TriggerDefinitionService triggerDefinitionService, WorkflowExecutor workflowExecutor,
        WorkflowService workflowService, IntegrationInstanceService integrationInstanceService,
        IntegrationWorkflowService integrationWorkflowService, WorkflowService workflowService1) {

        super(filesFileStorage, instanceAccessorRegistry, triggerDefinitionService, workflowExecutor, workflowService);

        this.connectedUserService = connectedUserService;
        this.httpServletRequest = httpServletRequest;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.workflowService = workflowService1;
    }

    @Override
    public ResponseEntity<Object> executeWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            environment, OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"));

        String workflowId = integrationWorkflowService.getLatestIntegrationWorkflowId(workflowReferenceCode);

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            connectedUser.getId(), workflowId, environment);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.EMBEDDED, integrationInstance.getId(), workflowReferenceCode, findRequestTriggerName(workflow));

        ResponseEntity<Object> responseEntity;

        if (!isWorkflowEnabled(workflowExecutionId)) {
            responseEntity = ResponseEntity.ok()
                .build();
        } else {
            try {
                responseEntity = doProcessTrigger(workflowExecutionId, httpServletRequest);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        }

        return responseEntity;
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private static String findRequestTriggerName(Workflow workflow) {
        return WorkflowTrigger.of(workflow)
            .stream()
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                if (Objects.equals(workflowNodeType.componentName(), "request")) {
                    return workflowTrigger.getName();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
