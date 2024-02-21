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

package com.bytechef.platform.configuration.domain;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

/**
 *
 * @author Ivica Cardic
 */
public record DataStream(ComponentType source, ComponentType destination) {

    public static DataStream of(Map<String, ?> extensions) {
        DataStream dataStream = null;

        if (extensions.containsKey(WorkflowExtConstants.SOURCE) ||
            extensions.containsKey(WorkflowExtConstants.DESTINATION)) {

            Map<String, ?> sourceMap = MapUtils.get(extensions, WorkflowExtConstants.SOURCE, new TypeReference<>() {});
            Map<String, ?> destinationMap = MapUtils.get(
                extensions, WorkflowExtConstants.DESTINATION, new TypeReference<>() {});

            dataStream = new DataStream(
                sourceMap == null ? null : toComponentType(sourceMap),
                destinationMap == null ? null : toComponentType(destinationMap));
        }

        return dataStream;
    }

    private static ComponentType toComponentType(Map<String, ?> componentTypeMap) {
        return new ComponentType(
            MapUtils.getRequiredString(componentTypeMap, WorkflowExtConstants.COMPONENT_NAME),
            MapUtils.getRequiredInteger(componentTypeMap, WorkflowExtConstants.COMPONENT_VERSION),
            MapUtils.getMap(componentTypeMap, WorkflowConstants.PARAMETERS, new TypeReference<>() {}, Map.of()));
    }

    public record ComponentType(String componentName, int componentVersion, Map<String, ?> parameters) {
    }
}
