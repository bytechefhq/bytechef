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
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes ActionDefinition over GraphQL.
 *
 * @author ByteChef
 */
@Controller
@ConditionalOnCoordinator
public class ActionDefinitionGraphQlController {

    private final ActionDefinitionService actionDefinitionService;

    public ActionDefinitionGraphQlController(ActionDefinitionService actionDefinitionService) {
        this.actionDefinitionService = actionDefinitionService;
    }

    @QueryMapping
    public ActionDefinition actionDefinition(
        @Argument String componentName, @Argument int componentVersion, @Argument String actionName) {

        return actionDefinitionService.getActionDefinition(componentName, componentVersion, actionName);
    }

    @QueryMapping
    public List<ActionDefinition> actionDefinitions(
        @Argument String componentName, @Argument int componentVersion) {

        return actionDefinitionService.getActionDefinitions(componentName, componentVersion);
    }
}
