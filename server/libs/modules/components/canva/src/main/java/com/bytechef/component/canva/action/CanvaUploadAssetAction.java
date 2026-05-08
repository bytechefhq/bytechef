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

package com.bytechef.component.canva.action;

import static com.bytechef.component.canva.constant.CanvaConstants.ASSET;
import static com.bytechef.component.canva.constant.CanvaConstants.ASSET_NAME;
import static com.bytechef.component.canva.constant.CanvaConstants.DELAY_MS;
import static com.bytechef.component.canva.constant.CanvaConstants.MAX_ATTEMPTS;
import static com.bytechef.component.canva.util.CanvaUtils.pollJob;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class CanvaUploadAssetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadAsset")
        .title("Upload Asset")
        .description("Get the status and results of an upload asset job.")
        .properties(
            string(ASSET_NAME)
                .label("Asset name")
                .description("The asset's name.")
                .minLength(1)
                .required(true),
            fileEntry(ASSET)
                .label("Asset")
                .description("Asset to upload.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("job")
                            .properties(
                                string("id")
                                    .description("The job ID."),
                                string("status")
                                    .description("The status of the job."),
                                object("asset")
                                    .properties(
                                        string("id").description("Asset ID."),
                                        string("type").description("Asset type."),
                                        string("name").description("Asset name."),
                                        array("tags")
                                            .items(string())
                                            .description("Asset tags."),
                                        object("owner")
                                            .properties(
                                                string("user_id").description("Owner user ID."),
                                                string("team_id").description("Owner team ID.")),
                                        string("created_at").description("Creation timestamp."),
                                        string("updated_at").description("Last update timestamp."),
                                        object("thumbnail")
                                            .properties(
                                                string("width").description("Thumbnail width."),
                                                string("height").description("Thumbnail height."),
                                                string("url").description("Thumbnail URL.")))))))
        .perform(CanvaUploadAssetAction::perform);

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object uploadJob = createUploadJob(inputParameters, context);

        if (!(uploadJob instanceof Map<?, ?> jobMap)) {
            throw new ProviderException("Canva upload asset action was not successful.");
        }

        return pollJob(context, "/asset-uploads/" + jobMap.get("id"), MAX_ATTEMPTS, DELAY_MS);
    }

    private static Object createUploadJob(Parameters inputParameters, Context context) {
        String base64Name = context.encoder(encoder -> encoder.base64Encode(
            inputParameters.getRequiredString(ASSET_NAME)));

        String header = context.json(json -> json.write(Map.of("name_base64", base64Name)));

        Map<String, Object> response = context
            .http(http -> http.post("/asset-uploads"))
            .header("Asset-Upload-Metadata", header)
            .body(Body.of(inputParameters.getRequiredFileEntry(ASSET), "application/octet-stream"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get("job");
    }
}
