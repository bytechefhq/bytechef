/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
@ConditionalOnEEVersion
public class ConnectedUserApiController implements ConnectedUserApi {

    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserApiController(ConnectedUserService connectedUserService,
        EnvironmentService environmentService) {
        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<Void> updateFrontendConnectedUser(
        EnvironmentModel xEnvironment, Map<String, Object> requestBody) {

        String externalUserId = ConnectedUserAuthentications.getCurrentExternalUserId();

        Environment environment = getEnvironment(xEnvironment);

        checkVisibilityAttributesUnchanged(externalUserId, environment, requestBody);

        connectedUserService.updateConnectedUser(externalUserId, environment, requestBody);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateConnectedUser(
        String externalUserId, EnvironmentModel xEnvironment, Map<String, Object> requestBody) {

        ConnectedUserAuthentications.requireCurrentExternalUserId(externalUserId);

        Environment environment = getEnvironment(xEnvironment);

        checkVisibilityAttributesUnchanged(externalUserId, environment, requestBody);

        connectedUserService.updateConnectedUser(externalUserId, environment, requestBody);

        return ResponseEntity.noContent()
            .build();
    }

    private void checkVisibilityAttributesUnchanged(
        String externalUserId, Environment environment, Map<String, Object> requestBody) {

        boolean apiKeyAuthenticated = ConnectedUserAuthentications.fetchCurrent()
            .map(ConnectedUserAuthentication::apiKeyAuthenticated)
            .orElse(false);

        if (apiKeyAuthenticated || requestBody == null || requestBody.isEmpty()) {
            return;
        }

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        Map<String, String> metadata = connectedUser.getMetadata();

        for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            boolean changed;

            if (key.equalsIgnoreCase("name") && value instanceof String name) {
                changed = !Objects.equals(name, connectedUser.getName());
            } else if (key.equalsIgnoreCase("email") && value instanceof String email) {
                changed = !Objects.equals(email, connectedUser.getEmail());
            } else {
                changed = !metadata.containsKey(key) ||
                    !Objects.equals(value == null ? null : value.toString(), metadata.get(key));
            }

            if (changed) {
                throw new AccessDeniedException(
                    "A connected user may not change their own name, email or metadata; only the API key may");
            }
        }
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
