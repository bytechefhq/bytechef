/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.PublishConnectedUserProjectWorkflowRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@ConditionalOnEEVersion
public class ConnectedUserProjectWorkflowApiController implements ConnectedUserProjectWorkflowApi {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowApiController(
        ConnectedUserProjectFacade connectedUserProjectFacade, ConversionService conversionService,
        EnvironmentService environmentService) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<Void> enableConnectedUserProjectWorkflow(
        String workflowReferenceCode, Boolean enable, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            SecurityUtils.getCurrentUserLogin(), workflowReferenceCode, enable,
            (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectedUserProjectWorkflowModel> getConnectedUserProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserProjectFacade.getConnectedUserProjectWorkflow(
                    SecurityUtils.getCurrentUserLogin(), workflowReferenceCode,
                    (long) getEnvironment(xEnvironment).ordinal()),
                ConnectedUserProjectWorkflowModel.class));
    }

    @Override
    public ResponseEntity<Void> publishConnectedUserProjectWorkflow(
        String workflowReferenceCode,
        PublishConnectedUserProjectWorkflowRequestModel publishConnectedUserProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.publishProjectWorkflow(
            SecurityUtils.getCurrentUserLogin(), workflowReferenceCode,
            publishConnectedUserProjectWorkflowRequestModel.getDescription(),
            (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
