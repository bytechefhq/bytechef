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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.ftp.constant.FtpConstants.CREATE_DIRECTORIES;
import static com.bytechef.component.ftp.constant.FtpConstants.NEW_PATH;
import static com.bytechef.component.ftp.constant.FtpConstants.OLD_PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.ftp.util.RemoteFileClient;
import java.io.IOException;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FtpRenameAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("rename")
        .title("Rename/Move")
        .description("Renames or moves a file or directory on the FTP/SFTP server.")
        .properties(
            string(OLD_PATH)
                .label("Source Path")
                .description("The current path of the file or directory.")
                .placeholder("/uploads/old-name.pdf")
                .required(true),
            string(NEW_PATH)
                .label("Destination Path")
                .description("The new path for the file or directory.")
                .placeholder("/archive/new-name.pdf")
                .required(true),
            bool(CREATE_DIRECTORIES)
                .label("Create Directories")
                .description("Create the destination directory structure if it does not exist.")
                .defaultValue(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("oldPath").description("The original path."),
                        string("newPath").description("The new path."),
                        bool("success").description("Whether the operation was successful."))),
            sampleOutput(
                Map.of(
                    "oldPath", "/uploads/old-name.pdf",
                    "newPath", "/archive/new-name.pdf",
                    "success", true)))
        .perform(FtpRenameAction::perform);

    private FtpRenameAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (RemoteFileClient remoteFileClient = RemoteFileClient.of(connectionParameters)) {
            String oldPath = inputParameters.getRequiredString(OLD_PATH);
            String newPath = inputParameters.getRequiredString(NEW_PATH);
            boolean createDirectories = inputParameters.getBoolean(CREATE_DIRECTORIES, false);

            if (createDirectories && newPath.contains("/")) {
                String directoryPath = newPath.substring(0, newPath.lastIndexOf('/'));

                remoteFileClient.createDirectoryTree(directoryPath);
            }

            remoteFileClient.rename(oldPath, newPath);

            return Map.of("oldPath", oldPath, "newPath", newPath, "success", true);
        } catch (IOException ioException) {
            throw new ProviderException("Failed to rename/move: " + ioException.getMessage(), ioException);
        }
    }
}
