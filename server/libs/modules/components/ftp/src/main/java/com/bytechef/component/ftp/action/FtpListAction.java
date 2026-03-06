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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FtpListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("list")
        .title("List Directory")
        .description("Lists the contents of a directory on the FTP/SFTP server.")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of the directory to list.")
                .placeholder("/uploads")
                .defaultValue("/")
                .required(true),
            bool(RECURSIVE)
                .label("Recursive")
                .description("List files recursively in subdirectories.")
                .defaultValue(false))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string("name").description("Name of the file or directory."),
                                string("path").description("Full path to the file or directory."),
                                string("type").description("Type: 'file' or 'directory'."),
                                integer("size").description("Size in bytes (for files)."),
                                string("modifiedAt").description("Last modified timestamp.")))))
        .perform(FtpListAction::perform);

    private FtpListAction() {
    }

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (RemoteFileClient remoteFileClient = RemoteFileClient.of(connectionParameters)) {
            String path = inputParameters.getRequiredString(PATH);
            boolean recursive = inputParameters.getBoolean(RECURSIVE, false);

            List<Map<String, Object>> results = new ArrayList<>();

            listFiles(remoteFileClient, path, recursive, results);

            return results;
        } catch (IOException ioException) {
            throw new ProviderException("Failed to list directory: " + ioException.getMessage(), ioException);
        }
    }

    private static void listFiles(
        RemoteFileClient remoteFileClient, String path, boolean recursive, List<Map<String, Object>> results)
        throws IOException {

        List<RemoteFileInfo> files = remoteFileClient.listFiles(path);

        for (RemoteFileInfo file : files) {
            String type = file.directory() ? "directory" : "file";

            Map<String, Object> fileInfo = new HashMap<>();

            fileInfo.put("name", file.name());
            fileInfo.put("path", file.path());
            fileInfo.put("type", type);
            fileInfo.put("size", file.size());
            fileInfo.put("modifiedAt", file.modifiedAt() != null ? file.modifiedAt()
                .toString() : "");

            results.add(fileInfo);

            if (recursive && file.directory()) {
                listFiles(remoteFileClient, file.path(), true, results);
            }
        }
    }
}
