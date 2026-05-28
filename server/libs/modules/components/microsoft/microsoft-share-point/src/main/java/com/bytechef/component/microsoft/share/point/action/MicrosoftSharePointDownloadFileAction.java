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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FILE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.LOCATION;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.REDIRECT_STATUS_CODE;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftSharePointDownloadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("downloadFile")
        .title("Download File")
        .description("Download file from Microsoft SharePoint site.")
        .properties(
            SITE_ID_PROPERTY,
            string(FILE_ID)
                .label("File ID")
                .description("ID of the file that will be downloaded.")
                .optionsLookupDependsOn(SITE_ID)
                .options((OptionsFunction<String>) MicrosoftSharePointUtils::getFileIdOptions)
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(MicrosoftSharePointDownloadFileAction::perform)
        .help(
            "", "https://docs.bytechef.io/reference/components/microsoft-share-point_v1#download-file")
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointDownloadFileAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Response response = context
            .http(http -> http.get(
                "/sites/%s/drive/items/%s/content".formatted(
                    inputParameters.getRequiredString(SITE_ID), inputParameters.getRequiredString(FILE_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        if (response.getStatusCode() == REDIRECT_STATUS_CODE) {
            String fileLocation = response.getFirstHeader(LOCATION);

            return context.http(http -> http.get(fileLocation))
                .configuration(Http.responseType(ResponseType.BINARY))
                .execute()
                .getBody(new TypeReference<>() {});
        }

        throw new ProviderException("File was not found.");
    }
}
