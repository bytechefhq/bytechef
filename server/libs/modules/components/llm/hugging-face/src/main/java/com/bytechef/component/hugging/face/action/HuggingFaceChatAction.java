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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.hugging.face.constant.HuggingFaceConstants.CHAT;
import static com.bytechef.component.hugging.face.constant.HuggingFaceConstants.CONTENT;
import static com.bytechef.component.hugging.face.constant.HuggingFaceConstants.MESSAGES;
import static com.bytechef.component.hugging.face.constant.HuggingFaceConstants.ROLE;
import static com.bytechef.component.hugging.face.constant.HuggingFaceConstants.URL;
import static util.LLMUtils.createMessage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import util.records.MessageRecord;

/**
 * @author Monika Domiter
 */
public class HuggingFaceChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CHAT)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            array(MESSAGES)
                .label("Messages")
                .description("A list of messages comprising the conversation so far.")
                .items(
                    object().properties(
                        string(CONTENT)
                            .label("Content")
                            .description("The contents of the message.")
                            .required(true),
                        string(ROLE)
                            .label("Role")
                            .description("The role of the messages author")
                            .options(
                                option("system", "system"),
                                option("user", "user"),
                                option("assistant", "assistant"),
                                option("tool", "tool"))
                            .required(true)))
                .required(true),
            string(URL)
                .label("URL")
                .description("Url of the inference endpoint"))
        .outputSchema(string())
        .perform(HuggingFaceChatAction::perform);

    private HuggingFaceChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        ChatModel chatModel = new HuggingfaceChatModel(connectionParameters.getString(TOKEN), inputParameters.getString(URL));
        List<MessageRecord> messageRecordList = inputParameters.getList(MESSAGES, new TypeReference<>() {});
        List<Message> messages = messageRecordList.stream()
            .map(messageRecord -> createMessage(messageRecord.getRole(), messageRecord.getContent()))
            .toList();

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult()
            .getOutput()
            .getContent();
    }
}
