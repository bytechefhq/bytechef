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
import static com.bytechef.component.ftp.constant.FtpConstants.PATH;
import static com.bytechef.component.ftp.constant.FtpConstants.RECURSIVE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.ftp.util.RemoteFileClient;
import com.bytechef.component.ftp.util.RemoteFileClient.RemoteFileInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FtpDeleteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("delete")
        .title("Delete")
        .description("Deletes a file or directory from the FTP/SFTP server.")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of the file or directory to delete.")
                .placeholder("/uploads/old-file.pdf")
                .required(true),
            bool(RECURSIVE)
                .label("Recursive")
                .description("If the path is a directory, delete all contents recursively.")
                .defaultValue(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("deletedPath").description("The path that was deleted."),
                        bool("success").description("Whether the deletion was successful."))),
            sampleOutput(Map.of("deletedPath", "/uploads/old-file.pdf", "success", true)))
        .perform(FtpDeleteAction::perform);

    private FtpDeleteAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (RemoteFileClient remoteFileClient = RemoteFileClient.of(connectionParameters)) {
            String path = inputParameters.getRequiredString(PATH);
            boolean recursive = inputParameters.getBoolean(RECURSIVE, false);

            if (remoteFileClient.isDirectory(path)) {
                if (recursive) {
                    deleteDirectoryRecursively(remoteFileClient, path);
                } else {
                    remoteFileClient.deleteDirectory(path);
                }
            } else {
                remoteFileClient.deleteFile(path);
            }

            return Map.of("deletedPath", path, "success", true);
        } catch (IOException ioException) {
            throw new ProviderException("Failed to delete: " + ioException.getMessage(), ioException);
        }
    }

    private static void deleteDirectoryRecursively(RemoteFileClient remoteFileClient, String directoryPath)
        throws IOException {

        List<RemoteFileInfo> files = remoteFileClient.listFiles(directoryPath);

        for (RemoteFileInfo file : files) {
            if (file.directory()) {
                deleteDirectoryRecursively(remoteFileClient, file.path());
            } else {
                remoteFileClient.deleteFile(file.path());
            }
        }

        remoteFileClient.deleteDirectory(directoryPath);
    }
}
