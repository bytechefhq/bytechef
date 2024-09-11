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

package com.bytechef.platform.configuration.workflow.connection;

import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class ScriptWorkflowConnectionFactory
    implements WorkflowConnectionFactory, WorkflowConnectionFactoryResolver {

    private final ComponentDefinitionService componentDefinitionService;

    public ScriptWorkflowConnectionFactory(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<WorkflowConnection> create(
        String workflowNodeName, Map<String, ?> extensions, ComponentDefinition componentDefinition) {

        return WorkflowConnection.of(
            extensions, workflowNodeName,
            (name, version) -> {
                ComponentDefinition curComponentDefinition = componentDefinitionService.getComponentDefinition(
                    name, version);

                return curComponentDefinition.isConnectionRequired();
            });
    }

    @Override
    public Optional<WorkflowConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(
            StringUtils.startsWith(componentDefinition.getName(), ScriptComponentDefinition.SCRIPT) ? this : null);
    }
}
