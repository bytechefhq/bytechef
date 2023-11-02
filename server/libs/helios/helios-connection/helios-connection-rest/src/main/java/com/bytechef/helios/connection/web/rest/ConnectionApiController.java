/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.helios.connection.web.rest;

import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.connection.dto.ConnectionDTO;
import com.bytechef.hermes.connection.facade.ConnectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class ConnectionApiController implements ConnectionApi {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectionApiController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ConnectionModel> createConnection(ConnectionModel connectionModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.create(
                    conversionService.convert(connectionModel, ConnectionDTO.class), ProjectConstants.PROJECT_TYPE),
                ConnectionModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(Long id) {
        connectionFacade.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectionModel> getConnection(Long id) {
        return ResponseEntity.ok(
            Validate.notNull(
                conversionService.convert(
                    connectionFacade.getConnection(Validate.notNull(id, "id")), ConnectionModel.class),
                "connection")
                .parameters(null));
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getConnections(
        String componentName, Integer connectionVersion, Long tagId) {

        return ResponseEntity.ok(
            connectionFacade.getConnections(componentName, connectionVersion, tagId, ProjectConstants.PROJECT_TYPE)
                .stream()
                .map(connection -> conversionService.convert(connection, ConnectionModel.class)
                    .parameters(null))
                .toList());
    }

    @Override
    public ResponseEntity<ConnectionModel> updateConnection(Long id, ConnectionModel connectionModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.update(
                    conversionService.convert(connectionModel.id(id), ConnectionDTO.class),
                    ProjectConstants.PROJECT_TYPE),
                ConnectionModel.class));
    }
}
