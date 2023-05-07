
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

package com.bytechef.hermes.definition.registry.component;

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.Property;

import java.util.List;

public interface ComponentDefinitionRegistry {

    ActionDefinition getActionDefinition(
        String actionName, String componentName, int componentVersion);

    List<? extends ActionDefinition> getActionDefinitions(String componentName, int componentVersion);

    Property<?> getActionProperty(
        String propertyName, String actionName, String componentName, int componentVersion);

    Authorization getAuthorization(String componentName, int connectionVersion, String authorizationName);

    ConnectionDefinition getComponentConnectionDefinition(String componentName, int connectionVersion);

    ComponentDefinition getComponentDefinition(String name, Integer version);

    List<? extends ComponentDefinition> getComponentDefinitions(String name);

    List<ComponentDefinition> getComponentDefinitions();

    ConnectionDefinition getConnectionDefinition(String componentName, int componentVersion);

    List<ConnectionDefinition> getConnectionDefinitions();

    List<ConnectionDefinition> getConnectionDefinitions(
        String componentName, int componentVersion);

    TriggerDefinition getTriggerDefinition(String triggerName, String componentName, int componentVersion);

    List<? extends TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion);

    Property<?> getTriggerProperty(
        String propertyName, String actionName, String componentName, int componentVersion);
}
