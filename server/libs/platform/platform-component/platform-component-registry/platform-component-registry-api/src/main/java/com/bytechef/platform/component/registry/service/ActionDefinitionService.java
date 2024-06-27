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

package com.bytechef.platform.component.registry.service;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionService {

    /**
     * Executes the routine for dynamic resolution of particular properties required for component action to properly
     * execute {@link #executeSingleConnectionPerform(String, int, String, Map, ComponentConnection, ActionContext)} or
     * {@link #executeMultipleConnectionsPerform(String, int, String, Map, Map, ActionContext)} methods. Duration is
     * unpredictable as it may require connecting to outer APIs/microservices/platforms. Method is only called in
     * designTime, never in runtime. Every change of lookupDependsOnPaths parameter triggers this method to
     * automatically update the dependent propertyName.
     *
     * @param componentName        the name of component
     * @param componentVersion     the version
     * @param actionName           action name
     * @param propertyName         name of the property that requires dynamic resolution and is of
     *                             type @{@link com.bytechef.component.definition.Property.DynamicPropertiesProperty}
     * @param inputParameters      key-value collection of parameters required by business logic
     * @param lookupDependsOnPaths list of dependent property names
     * @param connection           connection used to connect to outer source
     * @param context              additional technical data required by some actions
     * @return list of dynamically resolved properties
     * @throws {@link com.bytechef.platform.component.exception.ComponentExecutionException} - if procession breaks
     *                within ByteChef system or {@link com.bytechef.component.exception.ProviderException} - if external
     *                system is unavailable or call to it results in errors
     */
    List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection connection, @NonNull ActionContext context);

    Output executeMultipleConnectionsOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, ComponentConnection> connections,
        @NonNull ActionContext context);

    /**
     * Executes the action of particular component version which define perform function via
     * {@link com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction} interface. Duration is
     * unpredictable as work done by action may require connecting to outer APIs/microservices/platforms.
     *
     * @param componentName    the name of component
     * @param componentVersion the version
     * @param actionName       action name
     * @param inputParameters  key-value collection of parameters required by business logic
     * @param connections      collection of connections used to connect to outer sources
     * @param context          additional technical data required by some actions
     * @return result of execution or throws exceptions
     * @throws {@link com.bytechef.platform.component.exception.ComponentExecutionException} - if procession breaks
     *                within ByteChef system or {@link com.bytechef.component.exception.ProviderException} - if external
     *                system is unavailable or call to it results in errors
     */
    Object executeMultipleConnectionsPerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, ComponentConnection> connections,
        @NonNull ActionContext context);

    List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection connection, @NonNull ActionContext context);

    ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode, Object body,
        ActionContext actionContext);

    Output executeSingleConnectionOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context);

    /**
     * Executes the action of particular component version which define perform function via
     * {@link com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction} interface. Duration is
     * unpredictable as work done by action may require connecting to outer APIs/microservices/platforms.
     *
     * @param componentName    the name of component
     * @param componentVersion the version
     * @param actionName       action name
     * @param inputParameters  key-value collection of parameters required by business logic
     * @param connection       connection used to connect to outer sources
     * @param context          additional technical data required by some actions
     * @return result of execution or throws exceptions
     * @throws {@link com.bytechef.platform.component.exception.ComponentExecutionException} - if procession breaks
     *                within ByteChef system or {@link com.bytechef.component.exception.ProviderException} - if external
     *                system is unavailable or call to it results in errors
     */
    Object executeSingleConnectionPerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context);

    String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull ActionContext context);

    ActionDefinition getActionDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String actionName);

    List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion);

    boolean isSingleConnectionPerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName);
}
