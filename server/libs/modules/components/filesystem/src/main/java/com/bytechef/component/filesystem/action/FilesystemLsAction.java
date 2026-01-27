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

package com.bytechef.component.filesystem.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.RECURSIVE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;

/**
 * Filesystem list action for workflow automation. Lists files and directories at a specified path.
 *
 * @author Ivica Cardic
 */
public class FilesystemLsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("ls")
        .title("List")
        .description("Lists the content of a directory for the given path.")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of a directory.")
                .required(true),
            bool(RECURSIVE)
                .label("Recursive")
                .description("Should the subdirectories be included?")
                .defaultValue(false))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string("filename")
                                    .description("Name of the file."),
                                string("relativePath")
                                    .description("Relative path of the file."),
                                integer("size")
                                    .description("Size of the file.")))))
        .perform(FilesystemLsAction::perform);

    private FilesystemLsAction() {
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - Path traversal is intentional. The Filesystem component allows workflow
     * creators to list files/directories. Access is controlled through workflow-level permissions. The path is provided
     * by the workflow creator, not end users.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    protected static List<FileInfo> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws IOException {

        Path root = Paths.get(inputParameters.getRequiredString(PATH));
        boolean recursive = inputParameters.getBoolean(RECURSIVE, false);

        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(path -> filter(path, recursive, root))
                .filter(Files::isRegularFile)
                .map(path -> getFileInfo(path, root))
                .collect(Collectors.toList());
        }
    }

    private static boolean filter(Path path, boolean recursive, Path root) {
        Path parent = path.getParent();

        return recursive || parent.equals(root);
    }

    private static FileInfo getFileInfo(Path path, Path root) {
        File file = path.toFile();

        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Path does not pint to valid file");
        }

        return new FileInfo(
            String.valueOf(path.getFileName()), String.valueOf(root.relativize(path)), file.length());
    }

    public record FileInfo(String filename, String relativePath, long size) {

        public FileInfo {
            Validate.notNull(filename, "fileName is required");
            Validate.notNull(relativePath, "relativePath is required");
        }
    }
}
