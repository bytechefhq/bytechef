/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.CredentialStatusModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.EnvironmentModel;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
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
public class ConnectedUserApiController implements ConnectedUserApi {

    private final ConnectedUserFacade connectedUserFacade;
    private final ConnectedUserService connectedUserService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectedUserApiController(
        ConnectedUserFacade connectedUserFacade, ConnectedUserService connectedUserService,
        ConversionService conversionService) {

        this.connectedUserFacade = connectedUserFacade;
        this.connectedUserService = connectedUserService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> deleteConnectedUser(Long id) {
        connectedUserService.deleteConnectedUser(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectedUserModel> getConnectedUser(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(connectedUserFacade.getConnectedUser(id), ConnectedUserModel.class));
    }

    @Override
    public ResponseEntity<Page> getConnectedUsers(
        EnvironmentModel environment, CredentialStatusModel credentialStatus, LocalDate createDateFrom,
        LocalDate createDateTo, Long integrationId, Integer pageNumber, String search) {

        return ResponseEntity.ok(
            connectedUserFacade
                .getConnectedUsers(
                    environment == null ? null : Environment.valueOf(environment.name()),
                    search, credentialStatus == null ? null : CredentialStatus.valueOf(credentialStatus.name()),
                    createDateFrom, createDateTo, integrationId, pageNumber)
                .map(connectedUser -> conversionService.convert(connectedUser, ConnectedUserModel.class)));
    }

    @Override
    public ResponseEntity<Void> enableConnectedUser(Long id, Boolean enable) {
        connectedUserFacade.enableConnectedUser(id, enable);

        return ResponseEntity.noContent()
            .build();
    }
}
