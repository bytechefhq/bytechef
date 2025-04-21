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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionModel;
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
@ConditionalOnCoordinator
public class ClusterElementDefinitionApiController implements ClusterElementDefinitionApi {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConversionService conversionService;

    public ClusterElementDefinitionApiController(
        ClusterElementDefinitionService clusterElementDefinitionService, ConversionService conversionService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ClusterElementDefinitionModel> getComponentClusterElementDefinition(
        String componentName, Integer componentVersion, String clusterElementName) {

        return ResponseEntity.ok(
            conversionService.convert(
                clusterElementDefinitionService.getClusterElementDefinition(
                    componentName, componentVersion, clusterElementName),
                ClusterElementDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<ClusterElementDefinitionBasicModel>> getRootComponentClusterElementDefinitions(
        String rootComponentName, Integer rootComponentVersion, String clusterElementType) {

        return ResponseEntity.ok(
            clusterElementDefinitionService
                .getRootClusterElementDefinitions(rootComponentName, rootComponentVersion, clusterElementType)
                .stream()
                .map(clusterElementDefinition -> conversionService.convert(
                    clusterElementDefinition, ClusterElementDefinitionBasicModel.class))
                .toList());
    }
}
