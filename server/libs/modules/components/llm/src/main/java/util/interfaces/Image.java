package util.interfaces;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import util.records.ImageMessageRecord;
import util.records.MessageRecord;

import java.util.List;

import static constants.LLMConstants.MESSAGES;
import static util.LLMUtils.createMessage;

public interface Image {

    private static List<ImageMessage> getMessages(Parameters inputParameters) {

        List<ImageMessageRecord> imageMessageList = inputParameters.getList(MESSAGES, new Context.TypeReference<>() {});
        return imageMessageList.stream()
            .map(messageRecord -> new ImageMessage(messageRecord.getContent(), messageRecord.getWeight()))
            .toList();
    }

    static Object getResponse(Image image, Parameters inputParameters, Parameters connectionParameters) {
        ImageModel imageModel = image.createImageModel(inputParameters, connectionParameters);

        List<ImageMessage> messages = Image.getMessages(inputParameters);

        ImageResponse response = imageModel.call(new ImagePrompt(messages));
        return response.getResult()
            .getOutput();
    }

    ImageOptions createImageOptions(Parameters inputParameters);
    ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters);
}
