/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.ai.mcp.exception.McpAutomationWorkflowNotFoundException;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentications;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.McpAutomationWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class McpAutomationWorkflowApiController implements McpAutomationWorkflowApi {

    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;
    private final McpAutomationWorkflowFacade mcpAutomationWorkflowFacade;

    @SuppressFBWarnings("EI")
    public McpAutomationWorkflowApiController(
        ConnectedUserService connectedUserService, EnvironmentService environmentService,
        McpAutomationWorkflowFacade mcpAutomationWorkflowFacade) {

        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
        this.mcpAutomationWorkflowFacade = mcpAutomationWorkflowFacade;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> disableFrontendMcpAutomationWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        return updateMcpAutomationWorkflowEnabled(ConnectedUserAuthentications.getCurrentExternalUserId(), workflowUuid,
            xEnvironment, false);
    }

    @Override
    public ResponseEntity<Void> disableMcpAutomationWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        ConnectedUserAuthentications.requireCurrentExternalUserId(externalUserId);

        return updateMcpAutomationWorkflowEnabled(externalUserId, workflowUuid, xEnvironment, false);
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendMcpAutomationWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        return updateMcpAutomationWorkflowEnabled(ConnectedUserAuthentications.getCurrentExternalUserId(), workflowUuid,
            xEnvironment, true);
    }

    @Override
    public ResponseEntity<Void> enableMcpAutomationWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        ConnectedUserAuthentications.requireCurrentExternalUserId(externalUserId);

        return updateMcpAutomationWorkflowEnabled(externalUserId, workflowUuid, xEnvironment, true);
    }

    @ExceptionHandler(McpAutomationWorkflowNotFoundException.class)
    public ResponseEntity<Void> handleMcpAutomationWorkflowNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private ResponseEntity<Void> updateMcpAutomationWorkflowEnabled(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment, boolean enable) {

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(
            Objects.requireNonNull(connectedUser.getId()), workflowUuid, enable);

        return ResponseEntity.noContent()
            .build();
    }
}
