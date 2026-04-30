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

package com.bytechef.component.ftp.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.ftp.constant.FtpConstants.CREATE_DIRECTORIES;
import static com.bytechef.component.ftp.constant.FtpConstants.FILE_ENTRY;
import static com.bytechef.component.ftp.constant.FtpConstants.PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.ftp.util.RemoteFileClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FtpUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Uploads a file to the FTP/SFTP server.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("The file to upload.")
                .required(true),
            string(PATH)
                .label("Remote Path")
                .description("The path on the server where the file should be uploaded (including filename).")
                .placeholder("/uploads/document.pdf")
                .required(true),
            bool(CREATE_DIRECTORIES)
                .label("Create Directories")
                .description("Create the directory structure on the server if it does not exist.")
                .defaultValue(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("remotePath").description("The path where the file was uploaded."),
                        bool("success").description("Whether the upload was successful."))),
            sampleOutput(Map.of("remotePath", "/uploads/document.pdf", "success", true)))
        .perform(FtpUploadFileAction::perform);

    private FtpUploadFileAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (RemoteFileClient remoteFileClient = RemoteFileClient.of(connectionParameters)) {
            FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);
            String remotePath = inputParameters.getRequiredString(PATH);
            boolean createDirectories = inputParameters.getBoolean(CREATE_DIRECTORIES, false);

            if (createDirectories && remotePath.contains("/")) {
                String directoryPath = remotePath.substring(0, remotePath.lastIndexOf('/'));

                remoteFileClient.createDirectoryTree(directoryPath);
            }

            try (InputStream inputStream = context.file(file -> file.getInputStream(fileEntry))) {
                remoteFileClient.storeFile(remotePath, inputStream);

                return Map.of("remotePath", remotePath, "success", true);
            }
        } catch (IOException ioException) {
            throw new ProviderException("Failed to upload file: " + ioException.getMessage(), ioException);
        }
    }
}
