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

package com.bytechef.component.ai.llm.anthropic.action;

import static com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction.CHAT_MODEL;
import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.ASK_PROPERTIES;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.ai.llm.util.StreamChatUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.concurrent.Flow;

/**
 * @author Marko Kriskovic
 */
public class AnthropicStreamChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("streamAsk")
        .title("Ask (stream)")
        .description("Ask anything you want and stream the response.")
        .properties(ASK_PROPERTIES)
        .output(ModelUtils::output)
        .help("", "https://docs.bytechef.io/reference/components/anthropic_v1#ask-stream")
        .perform(AnthropicStreamChatAction::perform);

    private AnthropicStreamChatAction() {
    }

    public static SseEmitterHandler perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Flow.Publisher<?> publisher = CHAT_MODEL.stream(inputParameters, connectionParameters, context);

        return StreamChatUtils.createSseEmitterHandler(publisher, inputParameters, context);
    }
}
