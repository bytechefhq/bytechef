/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static com.bytechef.platform.component.definition.AppEventComponentDefinition.APP_EVENT;
import static com.bytechef.platform.component.definition.AppEventComponentDefinition.NEW_EVENT;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
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
public class AppEventTriggerApiController extends AbstractWebhookTriggerController implements AppEventTriggerApi {

    private final ConnectedUserService connectedUserService;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AppEventTriggerApiController(
        ApplicationProperties applicationProperties, ConnectedUserService connectedUserService,
        FilesFileStorage filesFileStorage, HttpServletRequest httpServletRequest,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowExecutor webhookWorkflowExecutor, HttpServletResponse httpServletResponse,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService, WorkflowService workflowService) {

        super(
            filesFileStorage, jobPrincipalAccessorRegistry, applicationProperties.getPublicUrl(),
            triggerDefinitionService, webhookWorkflowExecutor, workflowService);

        this.connectedUserService = connectedUserService;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.workflowService = workflowService;
    }

    public ResponseEntity<Void> executeWorkflows(EnvironmentModel xEnvironment) {
        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        List<IntegrationInstance> integrationInstances =
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUser.getId(), true);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<IntegrationInstanceWorkflow> integrationInstanceWorkflows =
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstance.getId());

            List<String> workflowIds = integrationInstanceWorkflows.stream()
                .filter(IntegrationInstanceWorkflow::isEnabled)
                .map(integrationInstanceWorkflow -> integrationInstanceConfigurationWorkflowService
                    .getIntegrationInstanceConfigurationWorkflow(
                        integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()))
                .map(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
                .toList();

            for (String workflowId : workflowIds) {
                Workflow workflow = workflowService.getWorkflow(workflowId);

                String appEventTriggerName = findAppEventTriggerName(workflow);

                if (appEventTriggerName == null) {
                    continue;
                }

                IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
                    workflowId);

                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    ModeType.EMBEDDED, integrationInstance.getId(), integrationWorkflow.getWorkflowReferenceCode(),
                    appEventTriggerName);

                try {
                    doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return ResponseEntity.ok()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private static String findAppEventTriggerName(Workflow workflow) {
        return WorkflowTrigger.of(workflow)
            .stream()
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                if (Objects.equals(workflowNodeType.name(), APP_EVENT) &&
                    Objects.equals(workflowNodeType.operation(), NEW_EVENT)) {

                    return workflowTrigger.getName();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
