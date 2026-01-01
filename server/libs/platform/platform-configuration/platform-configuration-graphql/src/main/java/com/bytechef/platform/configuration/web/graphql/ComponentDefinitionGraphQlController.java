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
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes ComponentDefinition over GraphQL.
 *
 * @author ByteChef
 */
@Controller
@ConditionalOnCoordinator
public class ComponentDefinitionGraphQlController {

    private final ComponentDefinitionService componentDefinitionService;
    private final UnifiedApiDefinitionService unifiedApiDefinitionService;

    public ComponentDefinitionGraphQlController(
        ComponentDefinitionService componentDefinitionService,
        UnifiedApiDefinitionService unifiedApiDefinitionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.unifiedApiDefinitionService = unifiedApiDefinitionService;
    }

    @QueryMapping
    public ComponentDefinition componentDefinition(
        @Argument String componentName, @Argument Integer componentVersion) {

        return componentDefinitionService.getComponentDefinition(componentName, componentVersion);
    }

    @QueryMapping
    public ComponentDefinition connectionComponentDefinition(
        @Argument String componentName, @Argument int connectionVersion) {

        return componentDefinitionService.getConnectionComponentDefinition(componentName, connectionVersion);
    }

    @QueryMapping
    public List<ComponentDefinition> componentDefinitionVersions(@Argument String componentName) {
        return componentDefinitionService.getComponentDefinitionVersions(componentName);
    }

    @QueryMapping
    public List<ComponentDefinition> unifiedApiComponentDefinitions(
        @Argument UnifiedApiDefinition.UnifiedApiCategory category) {

        return unifiedApiDefinitionService.getUnifiedApiComponentDefinitions(category);
    }
}
