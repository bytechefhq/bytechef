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

package com.bytechef.component.llm.test;

import static com.bytechef.component.llm.constants.LLMConstants.MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.llm.util.interfaces.Chat;
import com.bytechef.component.llm.util.records.MessageRecord;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Marko Kriskovic
 */
public abstract class ChatActionTest extends AbstractLLMActionTest {

    private static final String answer = "ANSWER";

    protected void performTest(ActionDefinition.SingleConnectionPerformFunction perform) {
        try (MockedStatic<Chat> mockedChat = Mockito.mockStatic(Chat.class)) {
            mockedChat.when(() -> Chat.getResponse(any(Chat.class), eq(mockedParameters), eq(mockedParameters)))
                .thenReturn(answer);

            String result = (String) perform.apply(mockedParameters, mockedParameters, mockedContext);

            assertEquals(answer, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void getResponseTest(ChatModel mockedChatModel){
        when(mockedParameters.getList(eq(MESSAGES), any(Context.TypeReference.class)))
            .thenReturn(List.of(new MessageRecord("QUESTION", "user")));

        Chat mockedChat = mock(Chat.class);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        ChatResponse mockedChatResponse = spy(chatResponse);

        when(mockedChat.createChatModel(mockedParameters, mockedParameters)).thenReturn(mockedChatModel);
        when(mockedChatModel.call(any(Prompt.class))).thenReturn(mockedChatResponse);

        Object response = Chat.getResponse(mockedChat, mockedParameters, mockedParameters);

        assertEquals(answer, response);
    }
}
