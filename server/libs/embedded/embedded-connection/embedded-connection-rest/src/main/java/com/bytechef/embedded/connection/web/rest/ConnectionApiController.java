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

package com.bytechef.embedded.connection.web.rest;

import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.web.rest.model.ConnectionModel;
import com.bytechef.platform.connection.web.rest.util.ConnectionApiControllerUtils;
import com.bytechef.platform.constant.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.embedded.connection.web.rest.ConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
@ConditionalOnEndpoint
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
                    conversionService.convert(connectionModel, ConnectionDTO.class),
                    Type.EMBEDDED),
                ConnectionModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(Long id) {
        return ConnectionApiControllerUtils.deleteConnection(id, connectionFacade);
    }

    @Override
    public ResponseEntity<ConnectionModel> getConnection(Long id) {
        return ConnectionApiControllerUtils.getConnection(id, connectionFacade, conversionService);
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getConnections(
        String componentName, Integer connectionVersion, Long tagId) {

        return ResponseEntity.ok(
            connectionFacade
                .getConnections(componentName, connectionVersion, tagId, Type.EMBEDDED)
                .stream()
                .map(connection -> conversionService.convert(connection, ConnectionModel.class)
                    .parameters(null))
                .toList());
    }

    @Override
    public ResponseEntity<ConnectionModel> updateConnection(Long id, ConnectionModel connectionModel) {
        return ConnectionApiControllerUtils.updateConnection(id, connectionModel, connectionFacade, conversionService);
    }
}
