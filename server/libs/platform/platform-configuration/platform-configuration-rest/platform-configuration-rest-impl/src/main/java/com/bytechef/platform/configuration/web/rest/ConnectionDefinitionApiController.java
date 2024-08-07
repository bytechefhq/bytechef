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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ConnectionDefinitionModel;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnEndpoint
public class ConnectionDefinitionApiController implements ConnectionDefinitionApi {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConversionService conversionService;

    public ConnectionDefinitionApiController(
        ConnectionDefinitionService connectionDefinitionService, ConversionService conversionService) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ConnectionDefinitionModel> getComponentConnectionDefinition(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectionDefinitionService.getConnectionDefinition(componentName, componentVersion),
                ConnectionDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<ConnectionDefinitionBasicModel>> getComponentConnectionDefinitions(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion)
                .stream()
                .map(connectionDefinition -> conversionService.convert(
                    connectionDefinition, ConnectionDefinitionBasicModel.class))
                .toList());
    }
}
