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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.constant.AppType;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ContextFactory {

    ActionContext createActionContext(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, AppType type,
        Long instanceId, Long instanceWorkflowId, Long jobId, ComponentConnection connection);

    Context createContext(
        @NonNull String componentName, ComponentConnection connection);

    TriggerContext createTriggerContext(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, AppType type,
        String workflowReferenceCode, ComponentConnection connection);
}
