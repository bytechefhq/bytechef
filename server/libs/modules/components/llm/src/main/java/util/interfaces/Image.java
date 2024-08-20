package util.interfaces;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import util.records.ImageMessageRecord;

import java.util.List;

import static constants.LLMConstants.IMAGE_MESSAGES;

public interface Image {

    private static List<ImageMessage> getMessages(Parameters inputParameters) {

        List<ImageMessageRecord> imageMessageList = inputParameters.getList(IMAGE_MESSAGES, new Context.TypeReference<>() {});
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
