/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static com.bytechef.ee.embedded.configuration.public_.web.rest.util.EnvironmentUtils.getEnvironment;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.ConnectedUserApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class ConnectedUserApiController implements ConnectedUserApi {

    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserApiController(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @Override
    public ResponseEntity<Void> updateConnectedUser(
        String externalUserId, EnvironmentModel xEnvironment, Map<String, Object> requestBody) {

        connectedUserService.updateConnectedUser(externalUserId, getEnvironment(xEnvironment), requestBody);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateFrontendConnectedUser(
        EnvironmentModel xEnvironment, Map<String, Object> requestBody) {

        String externalUserId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserService.updateConnectedUser(
            externalUserId, getEnvironment(xEnvironment), requestBody);

        return ResponseEntity.noContent()
            .build();
    }
}
