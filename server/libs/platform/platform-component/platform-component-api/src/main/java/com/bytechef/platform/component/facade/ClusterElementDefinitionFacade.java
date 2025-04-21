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

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.constant.ModeType;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementDefinitionFacade {

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId);

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ModeType type, @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId,
        @Nullable Long jobId, @Nullable String workflowId, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment);
}
