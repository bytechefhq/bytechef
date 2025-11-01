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

package com.bytechef.component.supabase.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.supabase.constant.SupabaseConstants.BUCKET_NAME;
import static com.bytechef.component.supabase.constant.SupabaseConstants.FILE;
import static com.bytechef.component.supabase.constant.SupabaseConstants.FILE_NAME;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.supabase.util.SupabaseUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SupabaseUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Upload file to Supabase Bucket.")
        .properties(
            string(BUCKET_NAME)
                .label("Bucket Name")
                .options((OptionsFunction<String>) SupabaseUtils::getBucketNameOptions)
                .required(true),
            string(FILE_NAME)
                .label("File Name")
                .description("Name of the file that will be uploaded.")
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .description("File you want to upload to Supabase.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("Key")
                            .description("Key of the file that was uploaded."),
                        string("Id")
                            .description("Id of the file that was uploaded."))))
        .perform(SupabaseUploadFileAction::perform);

    private SupabaseUploadFileAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post(
            "/storage/v1/object/%s/%s".formatted(
                inputParameters.getRequiredString(BUCKET_NAME), inputParameters.getRequiredString(FILE_NAME))))
            .body(
                Body.of(
                    inputParameters.getRequiredFileEntry(FILE)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
