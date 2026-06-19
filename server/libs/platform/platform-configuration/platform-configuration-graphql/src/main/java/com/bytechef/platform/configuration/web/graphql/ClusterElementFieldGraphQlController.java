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
import com.bytechef.platform.component.domain.Field;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Workflow-less variant of {@code clusterElementOptions} that surfaces the field/column list of a SOURCE or DESTINATION
 * cluster element via {@link com.bytechef.component.definition.datastream.FieldsProvider}. Used by the Add Context
 * Source wizard to populate the ID Field and Indexed Fields name dropdowns. Returns an empty list when the cluster
 * element does not implement {@code FieldsProvider}.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class ClusterElementFieldGraphQlController {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    ClusterElementFieldGraphQlController(ClusterElementDefinitionFacade clusterElementDefinitionFacade) {
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
    }

    @QueryMapping
    @PreAuthorize("#connectionId == null or hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_USE')")
    public List<Field> clusterElementFields(
        @Argument String componentName, @Argument int componentVersion, @Argument String clusterElementName,
        @Argument @Nullable Long connectionId, @Argument @Nullable Map<String, ?> inputParameters) {

        return clusterElementDefinitionFacade.executeFields(
            componentName, componentVersion, clusterElementName,
            inputParameters != null ? inputParameters : Map.of(), connectionId);
    }
}
