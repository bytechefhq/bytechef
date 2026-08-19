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
 * The single definition of "this workflow is reachable as a hosted chat".
 *
 * <p>
 * A chat trigger (type prefixed with {@code chat/}) can run in more than one mode. Only <em>hosted</em> mode — the mode
 * in which ByteChef itself serves the chat UI and accepts messages on the workflow's static webhook — makes a workflow
 * openable from a chat surface. Hosted is encoded as the first trigger's {@code parameters.mode} being absent (the
 * legacy default, written before the parameter existed) or equal to {@code 1}. Any other value, including a non-numeric
 * one, means the workflow is driven by an external chat channel and must not be offered as a hosted chat.
 * </p>
 *
 * <p>
 * The two halves are exposed separately on purpose. {@link #hasHostedChatTrigger(List)} is the product-level question
 * and is what every surface that lists chats must ask, so those lists stay mutually consistent: two sibling GraphQL
 * queries partition the same workflows into chat workflows and chat agents, and a workflow classified differently by
 * one of them would either vanish from both lists or appear in a list that cannot open it.
 * {@link #isChatTriggerType(String)} is only the trigger-type prefix test, for callers that have a type string but no
 * {@link WorkflowTrigger} instances to inspect — workflow validation walks the raw workflow JSON, and its warning about
 * chat-only approval channels asks nothing about mode.
 * </p>
 *
 * @author Ivica Cardic
 */
public class HostedChatTriggers {

    private static final String CHAT_TRIGGER_TYPE_PREFIX = "chat/";

    private static final String MODE = "mode";

    private static final int HOSTED_CHAT_MODE = 1;

    private HostedChatTriggers() {
    }

    /**
     * Whether the given triggers make their workflow a hosted chat workflow: at least one trigger's type starts with
     * {@code chat/}, and the FIRST trigger's {@code parameters.mode} is absent or equal to {@code 1}.
     *
     * <p>
     * Note that the two conditions are asked of potentially different triggers: the prefix test scans every trigger,
     * while the mode test looks only at the first one. An empty list is not a hosted chat.
     * </p>
     *
     * @param workflowTriggers the workflow's triggers, in workflow order
     * @return {@code true} if the workflow can be opened as a hosted chat
     */
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

    /**
     * Whether the given trigger type is a chat trigger type, regardless of the mode it is configured to run in.
     *
     * @param type the trigger type, may be {@code null}
     * @return {@code true} if the type starts with {@code chat/}
     */
    public static boolean isChatTriggerType(@Nullable String type) {
        return type != null && type.startsWith(CHAT_TRIGGER_TYPE_PREFIX);
    }
}
