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

package com.bytechef.component.filesystem.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.LS;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.RECURSIVE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
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
 * @author Ivica Cardic
 */
public class FilesystemLsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LS)
        .title("List")
        .description("Lists a content of directory for the given path.")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of a directory.")
                .required(true),
            bool(RECURSIVE)
                .label("Recursive")
                .description("Should subdirectories be included.")
                .defaultValue(false))
        .outputSchema(
            array()
                .items(
                    object()
                        .properties(
                            string("fileName"),
                            string("relativePath"),
                            integer("size"))))
        .perform(FilesystemLsAction::perform);

    protected static List<FileInfo> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

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

    public static class FileInfo {
        private final String fileName;
        private final String relativePath;
        private final long size;

        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        public FileInfo(String fileName, String relativePath, long size) {
            Validate.notNull(fileName, "fileName is required");
            Validate.notNull(relativePath, "relativePath is required");

            this.fileName = fileName;
            this.relativePath = relativePath;
            this.size = size;
        }

        public String getFilename() {
            return fileName;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public long getSize() {
            return size;
        }
    }
}
