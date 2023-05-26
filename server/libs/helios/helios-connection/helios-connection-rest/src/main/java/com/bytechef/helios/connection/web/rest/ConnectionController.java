
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.helios.connection.web.rest.model.ConnectionModel;
import com.bytechef.helios.connection.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.helios.connection.dto.ConnectionDTO;
import com.bytechef.helios.connection.facade.ConnectionFacade;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/automation")
public class ConnectionController implements ConnectionsApi {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectionController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ConnectionModel> createConnection(ConnectionModel connectionModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.create(conversionService.convert(connectionModel, ConnectionDTO.class)),
                ConnectionModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(Long id) {
        connectionFacade.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ConnectionModel> getConnection(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.getConnection(id), ConnectionModel.class)
                .parameters(null));
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getComponentConnections(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            connectionFacade.getConnections(componentName, componentVersion)
                .stream()
                .map(connection -> conversionService.convert(connection, ConnectionModel.class)
                    .parameters(null))
                .toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<List<ConnectionModel>> getConnections(
        List<String> componentNames, List<Long> tagIds) {

        return ResponseEntity.ok(
            connectionFacade.getConnections(componentNames, tagIds)
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
                    conversionService.convert(connectionModel.id(id), ConnectionDTO.class)),
                ConnectionModel.class));
    }

    @Override
    public ResponseEntity<Void> updateConnectionTags(Long id, UpdateTagsRequestModel updateConnectionTagsRequestModel) {
        List<TagModel> tagModels = updateConnectionTagsRequestModel.getTags();

        connectionFacade.update(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
