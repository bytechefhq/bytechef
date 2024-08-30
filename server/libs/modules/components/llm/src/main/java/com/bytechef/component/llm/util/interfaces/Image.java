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

package com.bytechef.component.llm.util.interfaces;

import static com.bytechef.component.llm.constants.LLMConstants.IMAGE_MESSAGES;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.records.ImageMessageRecord;
import java.util.List;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

/**
 * @author Marko Kriskovic
 */
public interface Image {

    private static List<ImageMessage> getMessages(Parameters inputParameters) {

        List<ImageMessageRecord> imageMessageList =
            inputParameters.getList(IMAGE_MESSAGES, new Context.TypeReference<>() {});
        return imageMessageList.stream()
            .map(messageRecord -> new ImageMessage(messageRecord.getContent(), messageRecord.getWeight()))
            .toList();
    }

    static Object getResponse(Image image, Parameters inputParameters, Parameters connectionParameters) {
        ImageModel imageModel = image.createImageModel(inputParameters, connectionParameters);

        List<ImageMessage> messages = getMessages(inputParameters);

        ImageResponse response = imageModel.call(new ImagePrompt(messages));
        ImageGeneration result = response.getResult();

        return result.getOutput();
    }

    ImageOptions createImageOptions(Parameters inputParameters);

    ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters);
}
