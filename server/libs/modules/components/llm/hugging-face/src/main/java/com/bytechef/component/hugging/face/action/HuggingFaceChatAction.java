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

package com.bytechef.component.hugging.face.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.hugging.face.constant.HuggingFaceConstants.URL;
import static com.bytechef.component.llm.constants.LLMConstants.ASK;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGE_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.interfaces.Chat;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.huggingface.HuggingfaceChatModel;

/**
 * @author Marko Kriskovic
 */
public class HuggingFaceChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(URL)
                .label("URL")
                .description("Url of the inference endpoint"),
            MESSAGE_PROPERTY)
        .outputSchema(object())
        .perform(HuggingFaceChatAction::perform);

    private HuggingFaceChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    public static final Chat CHAT = new Chat() {

        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return null;
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new HuggingfaceChatModel(connectionParameters.getString(TOKEN), inputParameters.getString(URL));
        }
    };
}
