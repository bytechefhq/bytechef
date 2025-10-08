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

package com.bytechef.platform.component.context;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.constant.ModeType;
import javax.annotation.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ContextFactory {

    ActionContext createActionContext(
        String componentName, int componentVersion, String actionName, @Nullable ModeType type,
        @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId,
        @Nullable String workflowId, @Nullable ComponentConnection connection, boolean editorEnvironment);

    Context createContext(String componentName, @Nullable ComponentConnection componentConnection);

    ClusterElementContext createClusterElementContext(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment);

    TriggerContext createTriggerContext(
        String componentName, int componentVersion, String triggerName, @Nullable ModeType type,
        @Nullable Long jobPrincipalId, @Nullable String workflowUuid, @Nullable ComponentConnection connection,
        boolean editorEnvironment);
}
