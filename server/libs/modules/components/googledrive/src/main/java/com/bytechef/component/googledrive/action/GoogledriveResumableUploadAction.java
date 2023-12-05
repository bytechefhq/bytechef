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

package com.bytechef.component.googledrive.action;

import static com.bytechef.component.googledrive.constant.GoogledriveConstants.RESUMABLEUPLOAD;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.component.googledrive.util.GoogleUtil;
import com.bytechef.component.googledrive.util.OAuthAuthentication;
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author Mario Cvjetojevic
 */
public final class GoogledriveResumableUploadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(RESUMABLEUPLOAD)
        .title("Upload file")
        .description("Uploads a file to google drive.")
        .properties()
        .outputSchema(string())
        .perform(GoogledriveResumableUploadAction::perform);

    protected static String perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        // Upload file photo.jpg on drive.
        File fileMetadata = new File();
        fileMetadata.setName("photo.jpg");

        java.io.File filePath = new java.io.File(new java.io.File("").getAbsolutePath());
        // Specify media type and file-path for file.
        FileContent mediaContent = new FileContent("image/jpeg", filePath);

        System.out.println("EXECUTING RESUMABLE UPLOAD !!!!!!!!!!!!!!!!!!!!!!!!!!!");

        // Build a new authorized API client service.
        Drive service = new Drive.Builder(new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            new OAuthAuthentication(connectionParameters.getRequiredString(ACCESS_TOKEN)))
            .setApplicationName("Drive samples")
            .build();


        try {
            File file = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
            return file.getId();
        } catch (GoogleJsonResponseException googleJsonResponseException) {
            throw new ComponentExecutionException("Unable to upload file " + inputParameters, googleJsonResponseException);
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to upload file " + inputParameters, ioException);
        }
    }

    public GoogledriveResumableUploadAction() {
    }
}
