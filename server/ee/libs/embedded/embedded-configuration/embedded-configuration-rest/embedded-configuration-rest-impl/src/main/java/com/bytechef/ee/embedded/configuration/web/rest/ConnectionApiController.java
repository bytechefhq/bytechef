/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateConnectionRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.ConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectionApiController implements ConnectionApi {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectionApiController(
        ConnectedUserConnectionFacade connectedUserConnectionFacade, ConnectionFacade connectionFacade,
        ConversionService conversionService) {

        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Long> createConnection(ConnectionModel connectionModel) {
        return ResponseEntity.ok(
            connectionFacade.create(
                conversionService.convert(connectionModel, ConnectionDTO.class), ModeType.EMBEDDED));
    }

    @Override
    public ResponseEntity<Long> createConnectedUserProjectWorkflowConnection(
        Long connectedUserId, String workflowUuid, ConnectionModel connectionModel) {

        return ResponseEntity.ok(
            connectedUserConnectionFacade.createConnectedUserProjectWorkflowConnection(
                connectedUserId, workflowUuid, conversionService.convert(connectionModel, ConnectionDTO.class)));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(Long id) {
        connectionFacade.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getConnectedUserConnections(
        Long connectedUserId, String componentName, List<Long> connectionIds) {

        return ResponseEntity.ok(
            connectedUserConnectionFacade
                .getConnections(connectedUserId, componentName, connectionIds == null ? List.of() : connectionIds)
                .stream()
                .map(this::toConnectionModel)
                .toList());
    }

    @Override
    public ResponseEntity<ConnectionModel> getConnection(Long id) {
        return ResponseEntity.ok(toConnectionModel(connectionFacade.getConnection(Validate.notNull(id, "id"))));
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getConnections(
        String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        return ResponseEntity.ok(
            connectionFacade
                .getConnections(componentName, connectionVersion, List.of(), tagId, environmentId, ModeType.EMBEDDED)
                .stream()
                .map(this::toConnectionModel)
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateConnection(Long id, UpdateConnectionRequestModel updateConnectionRequestModel) {
        List<Tag> list = updateConnectionRequestModel.getTags()
            .stream()
            .map(tagModel -> conversionService.convert(tagModel, Tag.class))
            .toList();

        connectionFacade.update(
            id, updateConnectionRequestModel.getName(), list,
            Objects.requireNonNull(updateConnectionRequestModel.getVersion()));

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressFBWarnings("NP")
    private ConnectionModel toConnectionModel(ConnectionDTO connection) {
        ConnectionModel connectionModel = conversionService.convert(connection, ConnectionModel.class);

        Objects.requireNonNull(connectionModel)
            .authorizationParameters(
                MapUtils.toMap(
                    connectionModel.getAuthorizationParameters(),
                    Map.Entry::getKey,
                    entry -> ObfuscateUtils.obfuscate(String.valueOf(entry.getValue()), 28, 8)));

        return Validate.notNull(connectionModel, "connectionModel")
            .parameters(null);
    }

}
