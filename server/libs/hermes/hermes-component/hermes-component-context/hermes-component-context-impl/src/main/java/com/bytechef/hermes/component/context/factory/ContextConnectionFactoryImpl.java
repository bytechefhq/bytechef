
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

package com.bytechef.hermes.component.context.factory;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.context.ContextConnectionImpl;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ContextConnectionFactoryImpl implements ContextConnectionFactory {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;

    public ContextConnectionFactoryImpl(
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public Connection createConnection(
        String componentName, int componentVersion, Map<String, ?> connectionParameters, String authorizationName) {

        ComponentDefinitionDTO componentDefinitionDTO = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        ConnectionDefinitionBasicDTO connectionDefinitionBasicDTO = OptionalUtils.get(
            componentDefinitionDTO.getConnection());

        return new ContextConnectionImpl(
            componentName, connectionDefinitionBasicDTO.getVersion(), connectionParameters, authorizationName,
            connectionDefinitionService);
    }
}
