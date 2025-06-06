/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.ConnectUserProjectFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.PublishConnectedUserProjectWorkflowRequestModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class ConnectedUserProjectWorkflowApiController implements ConnectedUserProjectWorkflowApi {

    private final ConnectUserProjectFacade connectUserProjectFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowApiController(
        ConnectUserProjectFacade connectUserProjectFacade, ConversionService conversionService) {

        this.connectUserProjectFacade = connectUserProjectFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> enableConnectedUserProjectWorkflow(
        String workflowReferenceCode, Boolean enable, EnvironmentModel xEnvironment) {

        connectUserProjectFacade.enableProjectWorkflow(, workflowReferenceCode, enable, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectedUserProjectWorkflowModel> getConnectedUserProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectUserProjectFacade.getProjectWorkflow(, workflowReferenceCode, getEnvironment(xEnvironment)),
                ConnectedUserProjectWorkflowModel.class));
    }

    @Override
    public ResponseEntity<Void> publishConnectedUserProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment,
        PublishConnectedUserProjectWorkflowRequestModel publishConnectedUserProjectWorkflowRequestModel) {

        connectUserProjectFacade.publishProjectWorkflow(,
            workflowReferenceCode,
            publishConnectedUserProjectWorkflowRequestModel.getDescription(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    private static Environment getEnvironment(EnvironmentModel xEnvironment) {
        return xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));
    }
}
