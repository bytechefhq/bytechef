package com.bytechef.component.llm.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.llm.util.interfaces.Chat;
import com.bytechef.component.llm.util.records.MessageRecord;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static com.bytechef.component.llm.constants.LLMConstants.MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class ChatActionTest extends AbstractLLMActionTest{
    private final String answer = "ANSWER";

    protected void performTest(ActionDefinition.SingleConnectionPerformFunction perform){
        try (MockedStatic<Chat> mockedChat = Mockito.mockStatic(Chat.class)) {
            mockedChat.when(() -> Chat.getResponse(any(Chat.class), eq(mockedParameters), eq(mockedParameters)))
                .thenReturn(answer);

            String result = (String) perform.apply(mockedParameters, mockedParameters, mockedContext);

            assertEquals(answer, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void getResponseResponse(ChatModel mockedChatModel){
        when(mockedParameters.getList(eq(MESSAGES), any(Context.TypeReference.class)))
            .thenReturn(List.of(new MessageRecord("QUESTION", "user")));

        Chat mockedChat = mock(Chat.class);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        ChatResponse mockedChatResponse = spy(chatResponse);

        when(mockedChat.createChatModel(mockedParameters, mockedParameters)).thenReturn(mockedChatModel);
        when(mockedChatModel.call(any(Prompt.class))).thenReturn(mockedChatResponse);

        String response = Chat.getResponse(mockedChat, mockedParameters, mockedParameters);

        assertEquals(answer, response);
    }
}
