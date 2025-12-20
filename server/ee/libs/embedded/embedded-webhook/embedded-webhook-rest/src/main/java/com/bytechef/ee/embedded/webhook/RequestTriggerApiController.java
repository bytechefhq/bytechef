/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.public_.web.rest.RequestTriggerApi;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.webhook.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class RequestTriggerApiController extends AbstractWebhookTriggerController implements RequestTriggerApi {

    private final ConnectedUserService connectedUserService;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowService workflowService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public RequestTriggerApiController(
        ApplicationProperties applicationProperties, ConnectedUserService connectedUserService,
        EnvironmentService environmentService, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        TempFileStorage tempFileStorage, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowExecutor webhookWorkflowExecutor, IntegrationInstanceService integrationInstanceService,
        IntegrationWorkflowService integrationWorkflowService, WorkflowService workflowService) {

        super(
            jobPrincipalAccessorRegistry, applicationProperties.getPublicUrl(), tempFileStorage,
            triggerDefinitionService, webhookWorkflowExecutor, workflowService);

        this.connectedUserService = connectedUserService;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.workflowService = workflowService;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<Object> executeWorkflow(String workflowUuid, EnvironmentModel xEnvironment) {
        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), environment);

        String workflowId = integrationWorkflowService.getLastWorkflowId(workflowUuid, environment);

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            connectedUser.getId(), workflowId, environment);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.EMBEDDED, integrationInstance.getId(), workflowUuid, findRequestTriggerName(workflow));

        ResponseEntity<Object> responseEntity;

        if (isWorkflowDisabled(workflowExecutionId)) {
            responseEntity = ResponseEntity.ok()
                .build();
        } else {
            try {
                responseEntity = doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse);
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

                if (Objects.equals(workflowNodeType.name(), "request")) {
                    return workflowTrigger.getName();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
