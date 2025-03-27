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

package com.bytechef.platform.component.service;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementDefinitionService {

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, Context context);

    ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName);

    List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType);

    List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType);

    <T> T getClusterElementObject(String componentName, int componentVersion, String clusterElementName);

    List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);
}
