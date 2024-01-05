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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.ActionDefinition;
import com.bytechef.hermes.component.registry.domain.ComponentConnection;
import com.bytechef.hermes.component.registry.domain.Property;
import com.bytechef.hermes.registry.domain.Option;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionService {

    List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, String searchText, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    Property executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    ActionDefinition getActionDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String actionName);

    List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion);

    List<ActionDefinition> getActionDefinitions(@NonNull List<OperationType> operationTypes);
}
