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

package com.bytechef.component.heygen.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.heygen.constant.HeyGenConstants.OUTPUT_LANGUAGE;
import static com.bytechef.component.heygen.constant.HeyGenConstants.TITLE;
import static com.bytechef.component.heygen.constant.HeyGenConstants.TRANSLATE_AUDIO_ONLY;
import static com.bytechef.component.heygen.constant.HeyGenConstants.VIDEO_URL;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.heygen.util.HeyGenUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class HeyGenTranslateVideoAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("translateVideoAction")
        .title("Translate Video")
        .description(
            "Translates a video into one or more of 175+ supported languages with natural-sounding voice and accurate lip-sync.")
        .properties(
            string(VIDEO_URL)
                .label("Video URL")
                .description(
                    "URL of the video file to be translated. Supports direct video file URLs, Google Drive URLs, and YouTube URLs.")
                .required(true),
            string(OUTPUT_LANGUAGE)
                .label("Output Language")
                .description("The target language in which the video will be translated.")
                .options((OptionsFunction<String>) HeyGenUtils::getLanguageOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Title of the video.")
                .required(false),
            bool(TRANSLATE_AUDIO_ONLY)
                .label("Translate Audio Only")
                .description(
                    "Translate only the audio; ideal for videos where the speaker is not visible, such as narrations, voiceovers, etc.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("video_translate_id")
                            .description("Unique identifier of the translated video."))))
        .perform(HeyGenTranslateVideoAction::perform);

    private HeyGenTranslateVideoAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, ?> body = context
            .http(http -> http.post("https://api.heygen.com/v2/video_translate"))
            .body(
                Body.of(
                    VIDEO_URL, inputParameters.getRequiredString(VIDEO_URL),
                    OUTPUT_LANGUAGE, inputParameters.getRequiredString(OUTPUT_LANGUAGE),
                    TITLE, inputParameters.getString(TITLE),
                    TRANSLATE_AUDIO_ONLY, inputParameters.getBoolean(TRANSLATE_AUDIO_ONLY)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get("data");
    }
}
