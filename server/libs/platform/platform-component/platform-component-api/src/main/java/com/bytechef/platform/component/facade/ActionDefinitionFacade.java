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

package com.bytechef.platform.component.facade;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionFacade {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId,
        @Nullable Long connectionId);

    default List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId) {

        return executeOptions(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, connectionId, null);
    }

    List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId, @Nullable String workflowId);

    default List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        Map<String, Long> connectionIds, Map<String, ?> extensions) {

        return executeOptions(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, connectionIds, extensions, null);
    }

    List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        Map<String, Long> connectionIds, Map<String, ?> extensions, @Nullable String workflowId);

    default OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds) {

        return executeOutput(componentName, componentVersion, actionName, inputParameters, connectionIds, null);
    }

    OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds, @Nullable String workflowId);

    Object executePerform(
        String componentName, int componentVersion, String actionName, @Nullable Long jobPrincipalId,
        @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId, @Nullable Long taskExecutionId,
        @Nullable String workflowId, Map<String, ?> inputParameters, Map<String, Long> connectionIds,
        Map<String, ?> extensions, @Nullable Long environmentId, @Nullable PlatformType type,
        boolean editorEnvironment, @Nullable Map<String, ?> continueParameters,
        @Nullable Map<String, ?> resumeData, @Nullable Instant suspendExpiresAt);
}
