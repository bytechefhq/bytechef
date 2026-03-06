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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.ftp.constant.FtpConstants.PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.ftp.util.RemoteFileClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Ivica Cardic
 */
public class FtpDownloadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("downloadFile")
        .title("Download File")
        .description("Downloads a file from the FTP/SFTP server.")
        .properties(
            string(PATH)
                .label("Remote Path")
                .description("The path of the file on the server to download.")
                .placeholder("/downloads/document.pdf")
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(FtpDownloadFileAction::perform);

    private FtpDownloadFileAction() {
    }

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (RemoteFileClient remoteFileClient = RemoteFileClient.of(connectionParameters)) {
            String remotePath = inputParameters.getRequiredString(PATH);
            String filename = remotePath.substring(remotePath.lastIndexOf('/') + 1);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                remoteFileClient.retrieveFile(remotePath, outputStream);

                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    return context.file(file -> file.storeContent(filename, inputStream));
                }
            }
        } catch (IOException ioException) {
            throw new ProviderException("Failed to download file: " + ioException.getMessage(), ioException);
        }
    }
}
