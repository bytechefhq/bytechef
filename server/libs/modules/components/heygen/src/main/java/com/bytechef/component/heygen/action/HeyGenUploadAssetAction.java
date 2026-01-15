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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.heygen.constant.HeyGenConstants.FILE_ENTRY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class HeyGenUploadAssetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadAssetAction")
        .title("Upload Asset")
        .description("Uploads a media file to the authenticated user's HeyGen account.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File Entry")
                .description("Asset file to upload.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("Unique identifier of the uploaded asset."),
                        string("name")
                            .description("ID assigned to the uploaded asset."),
                        string("file_type")
                            .description("Type of the uploaded asset, for example, audio, video, or image."),
                        string("folder_id")
                            .description("Unique identifier of the folder that contains the asset."),
                        string("meta")
                            .description("Metadata related to the uploaded asset."),
                        integer("created_ts")
                            .description("Unix timestamp when the asset was created."),
                        string("url")
                            .description("URL to access or download the uploaded file."),
                        string("image_key")
                            .description("Image key for image-type assets."))))
        .perform(HeyGenUploadAssetAction::perform);

    private HeyGenUploadAssetAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        FileEntry file = inputParameters.getFileEntry(FILE_ENTRY);

        Map<String, ?> body = context
            .http(http -> http.post("https://upload.heygen.com/v1/asset"))
            .header("Content-type", file.getMimeType())
            .body(
                Body.of(file))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get("data");
    }
}
