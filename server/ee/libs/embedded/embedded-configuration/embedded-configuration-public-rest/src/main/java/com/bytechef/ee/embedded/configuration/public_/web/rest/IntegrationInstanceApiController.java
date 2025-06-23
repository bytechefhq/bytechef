/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static com.bytechef.ee.embedded.configuration.public_.web.rest.util.EnvironmentUtils.getEnvironment;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendIntegrationInstanceRequestConnectionModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendIntegrationInstanceRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.IntegrationInstanceApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class IntegrationInstanceApiController implements IntegrationInstanceApi {

    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceApiController(ConnectedUserIntegrationFacade connectedUserIntegrationFacade) {
        this.connectedUserIntegrationFacade = connectedUserIntegrationFacade;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Long> createFrontendIntegrationInstance(
        Long id, EnvironmentModel xEnvironment,
        CreateFrontendIntegrationInstanceRequestModel createFrontendIntegrationInstanceRequestModel) {

        CreateFrontendIntegrationInstanceRequestConnectionModel connection =
            createFrontendIntegrationInstanceRequestModel.getConnection();

        String externalUserId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Map<String, Object> parameters = Objects.requireNonNull(connection)
            .getParameters();

        IntegrationInstance integrationInstance = connectedUserIntegrationFacade.createIntegrationInstance(
            externalUserId, id, parameters, getEnvironment(xEnvironment));

        return ResponseEntity.ok(integrationInstance.getId());
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> deleteFrontendIntegrationInstance(Long id) {
        String externalUserId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationFacade.deleteIntegrationInstance(externalUserId, id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Long> createIntegrationInstance(
        String externalUserId, Long id, EnvironmentModel xEnvironment,
        CreateFrontendIntegrationInstanceRequestModel createFrontendIntegrationInstanceRequestModel) {

        CreateFrontendIntegrationInstanceRequestConnectionModel connection =
            createFrontendIntegrationInstanceRequestModel.getConnection();

        Map<String, Object> parameters = Objects.requireNonNull(connection)
            .getParameters();

        IntegrationInstance integrationInstance = connectedUserIntegrationFacade.createIntegrationInstance(
            externalUserId, id, parameters, getEnvironment(xEnvironment));

        return ResponseEntity.ok(integrationInstance.getId());
    }

    @Override
    public ResponseEntity<Void> deleteIntegrationInstance(String externalUserId, Long id) {
        connectedUserIntegrationFacade.deleteIntegrationInstance(externalUserId, id);

        return ResponseEntity.noContent()
            .build();
    }
}
