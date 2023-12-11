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

package com.bytechef.hermes.component.registry.facade;

import com.bytechef.hermes.registry.domain.OptionsOutput;
import com.bytechef.hermes.registry.domain.ValueProperty;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionFacade {

    String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> actionParameters, Long connectionId);

    OptionsOutput executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, Object> inputParameters, Long connectionId, String searchText);

    List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> inputParameters, Long connectionId);

    Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, int type, Long instanceId,
        @NonNull String workflowId, long taskExecutionId, @NonNull Map<String, ?> inputParameters, Long connectionId);

    Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> inputParameters, Long connectionId);

    List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, Object> inputParameters, Long connectionId);
}
