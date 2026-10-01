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

package com.bytechef.component.chat;

import static com.bytechef.component.chat.constant.ChatConstants.CHAT;
import static com.bytechef.component.chat.constant.ChatConstants.MODE;
import static com.bytechef.component.chat.constant.ChatConstants.MODE_HOSTED_CHAT;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.chat.action.ChatResponseToRequestAction;
import com.bytechef.component.chat.cluster.ChatApprovalChannel;
import com.bytechef.component.chat.trigger.ChatNewRequestTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.stereotype.Component;

/**
 * Spring-declared rather than {@code @AutoService}, because the approval channel it ships needs an injected
 * {@link MessageBroker} to publish onto the run's SSE stream. Spring-declared handlers are always loaded, so the
 * component never depends on the build-time ServiceLoader index.
 *
 * @author Ivica Cardic
 */
@Component(CHAT + "_v1_ComponentHandler")
public class ChatComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public ChatComponentHandler(MessageBroker messageBroker) {
        this.componentDefinition = component(CHAT)
            .title("Chat")
            .description("Actions and triggers for using with the chat widget.")
            .icon("path:assets/chat.svg")
            .categories(ComponentCategory.HELPERS)
            .triggers(ChatNewRequestTrigger.TRIGGER_DEFINITION)
            .actions(ChatResponseToRequestAction.ACTION_DEFINITION)
            .clusterElements(ChatApprovalChannel.of(messageBroker))
            .agentChannels(
                agentChannel(
                    CHAT, ChatNewRequestTrigger.TRIGGER_DEFINITION, ChatResponseToRequestAction.ACTION_DEFINITION)
                        .title("Chat")
                        .approvalChannel(CHAT)
                        .triggerParameter(MODE, MODE_HOSTED_CHAT));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
