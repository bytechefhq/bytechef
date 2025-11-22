/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.web.rest.model.ConnectionModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateConnectionRequestModel;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.configuration.web.rest.ConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class ConnectionApiController implements ConnectionApi {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionApiController(
        ConnectionFacade connectionFacade, ConversionService conversionService,
        WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @Override
    public ResponseEntity<Long> createConnection(ConnectionModel connectionModel) {
        return ResponseEntity.ok(
            workspaceConnectionFacade.create(
                Objects.requireNonNull(connectionModel.getWorkspaceId()),
                conversionService.convert(connectionModel, ConnectionDTO.class)));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(Long id) {
        workspaceConnectionFacade.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectionModel> getConnection(Long id) {
        return ResponseEntity.ok(toConnectionModel(connectionFacade.getConnection(Validate.notNull(id, "id"))));
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getWorkspaceConnections(
        Long id, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        return ResponseEntity.ok(
            workspaceConnectionFacade
                .getConnections(id, componentName, connectionVersion, environmentId, tagId)
                .stream()
                .map(this::toConnectionModel)
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateConnection(
        Long id, UpdateConnectionRequestModel updateConnectionRequestModel) {
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
                ObfuscateUtils.toObfuscatedMap(
                    connectionModel.getAuthorizationParameters(), 28, 8));

        return Validate.notNull(connectionModel, "connectionModel")
            .parameters(null);
    }
}
