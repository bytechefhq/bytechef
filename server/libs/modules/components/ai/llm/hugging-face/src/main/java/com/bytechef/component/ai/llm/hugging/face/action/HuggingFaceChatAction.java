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

package com.bytechef.component.ai.llm.hugging.face.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.hugging.face.constant.HuggingFaceConstants.URL;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
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
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY)
        .output(LLMUtils::output)
        .perform(HuggingFaceChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters) -> new HuggingfaceChatModel(
        connectionParameters.getString(TOKEN), inputParameters.getString(URL));

    private HuggingFaceChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
