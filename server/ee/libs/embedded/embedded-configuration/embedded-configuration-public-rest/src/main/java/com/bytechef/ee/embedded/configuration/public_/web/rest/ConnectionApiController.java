/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectionModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
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
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.ConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectionApiController implements ConnectionApi {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectedUserService connectedUserService;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectionApiController(
        ConnectedUserConnectionFacade connectedUserConnectionFacade, ConnectedUserService connectedUserService,
        ConversionService conversionService, EnvironmentService environmentService) {

        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectedUserService = connectedUserService;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getConnections(
        String externalUserId, String componentName, EnvironmentModel xEnvironment, List<Long> connectionIds) {

        Environment environment = getEnvironment(xEnvironment);

        // TODO Move to facade

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return ResponseEntity.ok(
            connectedUserConnectionFacade
                .getConnections(connectedUser.getId(), componentName, connectionIds == null ? List.of() : connectionIds)
                .stream()
                .map(connectionDTO -> conversionService.convert(connectionDTO, ConnectionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getFrontendConnections(
        String componentName, EnvironmentModel xEnvironment, List<Long> connectionIds) {

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Environment environment = getEnvironment(xEnvironment);

        // TODO Move to facade

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return ResponseEntity.ok(
            connectedUserConnectionFacade
                .getConnections(connectedUser.getId(), componentName, connectionIds == null ? List.of() : connectionIds)
                .stream()
                .map(connectionDTO -> conversionService.convert(connectionDTO, ConnectionModel.class))
                .toList());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return xEnvironment == null ? Environment.PRODUCTION : environmentService.getEnvironment(xEnvironment.name());
    }
}
