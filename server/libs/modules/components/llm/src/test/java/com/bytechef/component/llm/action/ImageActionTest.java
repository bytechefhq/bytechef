package com.bytechef.component.llm.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.llm.util.interfaces.Chat;
import com.bytechef.component.llm.util.interfaces.Image;
import com.bytechef.component.llm.util.records.MessageRecord;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;

import java.util.List;

import static com.bytechef.component.llm.constants.LLMConstants.MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class ImageActionTest extends AbstractLLMActionTest{
    private final org.springframework.ai.image.Image answer = new org.springframework.ai.image.Image("url", "b64JSON");

    protected void performTest(ActionDefinition.SingleConnectionPerformFunction perform){
        try (MockedStatic<Image> mockedImage = Mockito.mockStatic(Image.class)) {
            mockedImage.when(() -> Image.getResponse(any(Image.class), eq(mockedParameters), eq(mockedParameters)))
                .thenReturn(answer);

            org.springframework.ai.image.Image result = (org.springframework.ai.image.Image) perform.apply(mockedParameters, mockedParameters, mockedContext);

            assertEquals(answer, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void getResponseTest(ImageModel mockedImageModel){
        when(mockedParameters.getList(eq(MESSAGES), any(Context.TypeReference.class)))
            .thenReturn(List.of(new ImageMessage("PROMPT", 1f)));

        Image mockedImage = mock(Image.class);
        ImageResponse imageResponse = new ImageResponse(List.of(new ImageGeneration(answer)));
        ImageResponse mockedImageResponse = spy(imageResponse);

        when(mockedImage.createImageModel(mockedParameters, mockedParameters)).thenReturn(mockedImageModel);
        when(mockedImageModel.call(any(ImagePrompt.class))).thenReturn(mockedImageResponse);

        org.springframework.ai.image.Image response = (org.springframework.ai.image.Image) Image.getResponse(mockedImage, mockedParameters, mockedParameters);

        assertEquals(answer, response);
    }
}
