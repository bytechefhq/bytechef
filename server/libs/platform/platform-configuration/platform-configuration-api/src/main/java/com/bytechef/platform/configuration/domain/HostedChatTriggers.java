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

package com.bytechef.platform.configuration.domain;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class HostedChatTriggers {

    private static final String CHAT_TRIGGER_TYPE_PREFIX = "chat/";

    private static final String MODE = "mode";

    private static final int HOSTED_CHAT_MODE = 1;

    private HostedChatTriggers() {
    }

    public static boolean hasHostedChatTrigger(List<WorkflowTrigger> workflowTriggers) {
        boolean hasChatTrigger = workflowTriggers.stream()
            .map(WorkflowTrigger::getType)
            .anyMatch(HostedChatTriggers::isChatTriggerType);

        if (!hasChatTrigger) {
            return false;
        }

        WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

        Map<String, ?> parameters = workflowTrigger.getParameters();

        Object mode = parameters.get(MODE);

        if (mode == null) {
            return true;
        }

        return mode instanceof Number number && number.intValue() == HOSTED_CHAT_MODE;
    }

    public static boolean isChatTriggerType(@Nullable String type) {
        return type != null && type.startsWith(CHAT_TRIGGER_TYPE_PREFIX);
    }
}
