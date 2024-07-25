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

package com.bytechef.platform.component.registry.facade;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.constant.AppType;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionFacade extends OperationDefinitionFacade {

    List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, Long connectionId);

    List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId);

    Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, Long> connectionIds);

    Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, AppType type, Long instanceId,
        Long instanceWorkflowId, Long jobId, @NonNull Map<String, ?> inputParameters,
        @NonNull Map<String, Long> connectionIds);

    Object executePerformForPolyglot(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection componentConnection,
        @NonNull ActionContext actionContext);

    String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters);
}
