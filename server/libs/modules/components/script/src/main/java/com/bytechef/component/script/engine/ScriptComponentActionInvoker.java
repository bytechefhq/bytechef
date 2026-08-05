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

package com.bytechef.component.script.engine;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.service.ActionDefinitionService;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.springframework.context.ApplicationContext;

/**
 * {@link ComponentActionInvoker} adapter backing the script component's {@code context.component.<name>.<action>}
 * calls. Resolves the connection from the task's configured {@code componentConnections} (by name, or the first
 * connection matching the target component when none is given) and dispatches through
 * {@link ActionDefinitionService#executePerformForPolyglot}, matching the pre-extraction {@code PolyglotEngine}
 * behavior verbatim, including lazy bean resolution of {@link ActionDefinitionService} from the
 * {@link ApplicationContext} on every call.
 *
 * @author Ivica Cardic
 */
class ScriptComponentActionInvoker implements ComponentActionInvoker {

    private final ApplicationContext applicationContext;
    private final Map<String, ComponentConnection> componentConnections;
    private final JobContextAware jobContextAware;
    private final ScriptComponentCatalog componentCatalog;

    ScriptComponentActionInvoker(
        ApplicationContext applicationContext, Map<String, ComponentConnection> componentConnections,
        JobContextAware jobContextAware, ScriptComponentCatalog componentCatalog) {

        this.applicationContext = applicationContext;
        this.componentConnections = componentConnections;
        this.jobContextAware = jobContextAware;
        this.componentCatalog = componentCatalog;
    }

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public Object invoke(String componentName, String actionName, Map<String, ?> input, String connectionName) {
        ComponentDefinition componentDefinition = componentCatalog.getComponentDefinition(componentName);

        ComponentConnection componentConnection = null;

        if (!componentConnections.isEmpty()) {
            Map.Entry<String, ComponentConnection> entry;

            if (connectionName == null) {
                entry = getFirstComponentConnectionEntry(componentDefinition);
            } else {
                entry = getComponentConnectionEntry(connectionName);
            }

            if (entry != null) {
                componentConnection = entry.getValue();
            }
        }

        ActionDefinitionService actionDefinitionService = applicationContext.getBean(ActionDefinitionService.class);

        ActionContext newActionContext = jobContextAware.toActionContext(
            componentDefinition.getName(), componentDefinition.getVersion(), actionName, componentConnection);

        return actionDefinitionService.executePerformForPolyglot(
            componentDefinition.getName(), componentDefinition.getVersion(), actionName, (Map) input,
            componentConnection, jobContextAware.getEnvironmentId(), newActionContext);
    }

    private Map.Entry<String, ComponentConnection> getComponentConnectionEntry(String connectionName) {
        return componentConnections
            .entrySet()
            .stream()
            .filter(entry -> Objects.equals(entry.getKey(), connectionName))
            .findFirst()
            .map(entry -> Map.entry(entry.getKey(), toComponentConnection(entry.getValue())))
            .orElseThrow(() -> new IllegalArgumentException(
                "Connection with name %s does not exist".formatted(connectionName)));
    }

    private @Nullable Map.Entry<String, ComponentConnection> getFirstComponentConnectionEntry(
        ComponentDefinition componentDefinition) {

        return componentConnections.entrySet()
            .stream()
            .filter(entry -> {
                ComponentConnection componentConnection = entry.getValue();

                return Objects.equals(componentConnection.getComponentName(), componentDefinition.getName());
            })
            .findFirst()
            .map(entry -> Map.entry(entry.getKey(), toComponentConnection(entry.getValue())))
            .orElse(null);
    }

    private ComponentConnection toComponentConnection(ComponentConnection componentConnection) {
        return new ComponentConnection(
            componentConnection.getComponentName(), componentConnection.getVersion(),
            componentConnection.getConnectionId(), componentConnection.getParameters(),
            componentConnection.getAuthorizationType());
    }

}
