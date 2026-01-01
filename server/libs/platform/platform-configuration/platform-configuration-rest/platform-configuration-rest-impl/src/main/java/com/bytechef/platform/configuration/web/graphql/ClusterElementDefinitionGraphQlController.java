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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes ClusterElementDefinition over GraphQL.
 *
 * @author ByteChef
 */
@Controller
@ConditionalOnCoordinator
public class ClusterElementDefinitionGraphQlController {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public ClusterElementDefinitionGraphQlController(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    @QueryMapping
    public ClusterElementDefinition clusterElementDefinition(
        @Argument String componentName, @Argument int componentVersion, @Argument String clusterElementName) {

        return clusterElementDefinitionService.getClusterElementDefinition(
            componentName, componentVersion, clusterElementName);
    }

    @QueryMapping
    public List<ClusterElementDefinition> clusterElementDefinitions(
        @Argument String rootComponentName, @Argument int rootComponentVersion, @Argument String clusterElementType) {

        return clusterElementDefinitionService.getRootClusterElementDefinitions(
            rootComponentName, rootComponentVersion, clusterElementType);
    }
}
