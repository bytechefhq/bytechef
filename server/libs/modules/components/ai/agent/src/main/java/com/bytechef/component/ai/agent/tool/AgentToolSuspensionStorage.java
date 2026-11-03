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

package com.bytechef.component.ai.agent.tool;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class AgentToolSuspensionStorage {

    public static final String DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY = "__BYTECHEF_AGENT_TOOL_SUSPENSION_KEY__";

    private static final String DATA_STORAGE_KEY_PREFIX = "__AGENT_TOOL_SUSPENSION_";

    private static final Logger log = LoggerFactory.getLogger(AgentToolSuspensionStorage.class);

    private AgentToolSuspensionStorage() {
    }

    public static void put(
        Map<String, Object> continueParameters, AgentToolSuspension agentToolSuspension,
        ActionContextAware actionContextAware) {

        continueParameters.remove(AgentToolSuspension.CONTINUE_PARAMETER_KEY);
        continueParameters.remove(DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY);

        String dataStorageKey = getDataStorageKey(actionContextAware);

        if (dataStorageKey != null && store(actionContextAware, dataStorageKey, agentToolSuspension)) {
            continueParameters.put(DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY, dataStorageKey);
        } else {
            continueParameters.put(AgentToolSuspension.CONTINUE_PARAMETER_KEY, agentToolSuspension);
        }
    }

    public static @Nullable AgentToolSuspension fetch(Parameters continueParameters, ActionContext context) {
        String dataStorageKey = continueParameters.getString(DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY);

        if (dataStorageKey == null) {
            return continueParameters.get(AgentToolSuspension.CONTINUE_PARAMETER_KEY, AgentToolSuspension.class);
        }

        Optional<Object> storedValueOptional =
            context.data(data -> data.fetch(Scope.CURRENT_EXECUTION, dataStorageKey));

        return storedValueOptional
            .map(storedValue -> ConvertUtils.convertValue(storedValue, AgentToolSuspension.class))
            .orElse(null);
    }

    public static boolean isStored(Map<String, ?> continueParameters) {
        return continueParameters.containsKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY) ||
            continueParameters.containsKey(DATA_STORAGE_KEY_CONTINUE_PARAMETER_KEY);
    }

    private static @Nullable String getDataStorageKey(ActionContextAware actionContextAware) {
        Long taskExecutionId = actionContextAware.getTaskExecutionId();

        if (actionContextAware.isEditorEnvironment() || actionContextAware.getJobId() == null ||
            taskExecutionId == null || actionContextAware.getEnvironmentId() == null ||
            actionContextAware.getPlatformType() == null) {

            return null;
        }

        return DATA_STORAGE_KEY_PREFIX + taskExecutionId;
    }

    private static boolean store(
        ActionContextAware actionContextAware, String dataStorageKey, AgentToolSuspension agentToolSuspension) {

        Map<?, ?> value = ConvertUtils.convertValue(agentToolSuspension, Map.class);

        return actionContextAware.data(data -> {
            try {
                data.put(Scope.CURRENT_EXECUTION, dataStorageKey, value);

                return true;
            } catch (IllegalArgumentException exception) {
                log.warn(
                    "The agent conversation could not be stored in Data Storage, so it is kept in the suspend",
                    exception);

                return false;
            }
        });
    }
}
