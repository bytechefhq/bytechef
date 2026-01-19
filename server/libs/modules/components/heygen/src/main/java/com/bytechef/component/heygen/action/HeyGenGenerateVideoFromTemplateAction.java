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
import static com.bytechef.component.heygen.constant.HeyGenConstants.CAPTION;
import static com.bytechef.component.heygen.constant.HeyGenConstants.ENABLE_SHARING;
import static com.bytechef.component.heygen.constant.HeyGenConstants.FOLDER_ID;
import static com.bytechef.component.heygen.constant.HeyGenConstants.TEMPLATE_ID;

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
public class HeyGenGenerateVideoFromTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("generateVideoFromTemplateAction")
        .title("Generate Video From Template")
        .description("Generates a video based on the specified template.")
        .properties(
            string(TEMPLATE_ID)
                .label("Template ID")
                .description("The ID of the template.")
                .options((OptionsFunction<String>) HeyGenUtils::getTemplateIdOptions)
                .required(true),
            string(FOLDER_ID)
                .label("Folder ID")
                .description("Unique identifier of the folder where the video is stored.")
                .options((OptionsFunction<String>) HeyGenUtils::getFolderIdOptions)
                .required(false),
            bool(CAPTION)
                .label("Caption")
                .description("Whether to enable captions in the video.")
                .required(false),
            bool(ENABLE_SHARING)
                .label("Enable Sharing")
                .description("Whether to make the video publicly shareable immediately after creation.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("video_id")
                            .description("Unique identifier of the generated video."))))
        .perform(HeyGenGenerateVideoFromTemplateAction::perform);

    private HeyGenGenerateVideoFromTemplateAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, ?> body = context
            .http(http -> http.post(
                "https://api.heygen.com/v2/template/" + inputParameters.getRequiredString(TEMPLATE_ID) + "/generate"))
            .body(
                Body.of(
                    FOLDER_ID, inputParameters.getString(FOLDER_ID),
                    CAPTION, inputParameters.getBoolean(CAPTION),
                    ENABLE_SHARING, inputParameters.getBoolean(ENABLE_SHARING)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get("data");
    }
}
