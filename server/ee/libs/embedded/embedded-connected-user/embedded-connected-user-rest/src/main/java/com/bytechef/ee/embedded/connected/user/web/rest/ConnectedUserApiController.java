/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserImportRejectedRowModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserImportResultModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.CreateConnectedUserRequestModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.CredentialStatusModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.UpdateConnectedUserRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserApiController implements ConnectedUserApi {

    private final ConnectedUserFacade connectedUserFacade;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserApiController(
        ConnectedUserFacade connectedUserFacade, ConversionService conversionService,
        EnvironmentService environmentService) {

        this.connectedUserFacade = connectedUserFacade;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<ConnectedUserModel> createConnectedUser(
        CreateConnectedUserRequestModel createConnectedUserRequestModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserFacade.createConnectedUser(
                    createConnectedUserRequestModel.getExternalId(),
                    environmentService.getEnvironment(createConnectedUserRequestModel.getEnvironmentId()),
                    createConnectedUserRequestModel.getName(), createConnectedUserRequestModel.getEmail(),
                    createConnectedUserRequestModel.getMetadata()),
                ConnectedUserModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteConnectedUser(Long id) {
        connectedUserFacade.deleteConnectedUser(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectedUserModel> getConnectedUser(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(connectedUserFacade.getConnectedUserDTO(id), ConnectedUserModel.class));
    }

    @Override
    public ResponseEntity<Void> enableConnectedUser(Long id, Boolean enable) {
        connectedUserFacade.enableConnectedUser(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Page> getConnectedUsers(
        Long environmentId, CredentialStatusModel credentialStatus, LocalDate createDateFrom,
        LocalDate createDateTo, Long integrationId, Integer pageNumber, String search) {

        return ResponseEntity.ok(
            connectedUserFacade
                .getConnectedUserDTOs(
                    environmentId, search,
                    credentialStatus == null ? null : CredentialStatus.valueOf(credentialStatus.name()),
                    createDateFrom, createDateTo, integrationId, pageNumber)
                .map(connectedUser -> conversionService.convert(connectedUser, ConnectedUserModel.class)));
    }

    @Override
    public ResponseEntity<ConnectedUserImportResultModel> importConnectedUsers(
        Long environmentId, MultipartFile file) {

        Environment environment = environmentService.getEnvironment(environmentId);

        try (InputStream inputStream = file.getInputStream()) {
            return ResponseEntity.ok(
                toImportResultModel(connectedUserFacade.importConnectedUsers(environment, inputStream)));
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    @Override
    public ResponseEntity<ConnectedUserModel> updateConnectedUser(
        Long id, UpdateConnectedUserRequestModel updateConnectedUserRequestModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserFacade.updateConnectedUser(
                    id, updateConnectedUserRequestModel.getName(), updateConnectedUserRequestModel.getEmail(),
                    updateConnectedUserRequestModel.getMetadata(), updateConnectedUserRequestModel.getVersion()),
                ConnectedUserModel.class));
    }

    private static ConnectedUserImportResultModel toImportResultModel(ConnectedUserImportResultDTO resultDTO) {
        List<ConnectedUserImportRejectedRowModel> rejectedRowModels = resultDTO.rejectedRows()
            .stream()
            .map(
                rejectedRow -> new ConnectedUserImportRejectedRowModel()
                    .rowNumber(rejectedRow.rowNumber())
                    .reason(rejectedRow.reason()))
            .toList();

        return new ConnectedUserImportResultModel()
            .createdCount(resultDTO.createdCount())
            .skippedExternalIds(resultDTO.skippedExternalIds())
            .rejectedRows(rejectedRowModels);
    }
}
