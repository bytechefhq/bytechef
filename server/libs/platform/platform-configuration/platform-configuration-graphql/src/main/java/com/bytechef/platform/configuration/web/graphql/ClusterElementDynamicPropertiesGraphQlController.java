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
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class ClusterElementDynamicPropertiesGraphQlController {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    ClusterElementDynamicPropertiesGraphQlController(ClusterElementDefinitionFacade clusterElementDefinitionFacade) {
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
    }

    @QueryMapping
    public List<Property> clusterElementDynamicProperties(
        @Argument String componentName, @Argument int componentVersion, @Argument String clusterElementName,
        @Argument String propertyName, @Argument Long connectionId, @Argument Map<String, ?> inputParameters,
        @Argument List<String> lookupDependsOnPaths) {

        return clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName,
            inputParameters != null ? inputParameters : Map.of(),
            lookupDependsOnPaths != null ? lookupDependsOnPaths : List.of(),
            connectionId);
    }
}
