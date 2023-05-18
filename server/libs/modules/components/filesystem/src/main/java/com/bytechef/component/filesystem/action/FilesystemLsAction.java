
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.MapValueUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.LS;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.RECURSIVE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class FilesystemLsAction {

    public static final ActionDefinition ACTION_DEFINITION = action(LS)
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
        .execute(FilesystemLsAction::executeLs);

    protected static List<FileInfo> executeLs(Context context, Map<String, ?> inputParameters) {
        Path root = Paths.get(MapValueUtils.getRequiredString(inputParameters, PATH));

        boolean recursive = MapValueUtils.getBoolean(inputParameters, RECURSIVE, false);

        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> recursive || p.getParent()
                .equals(root))
                .filter(Files::isRegularFile)
                .map(p -> new FileInfo(root, p))
                .collect(Collectors.toList());
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to list directory entries", ioException);
        }
    }

    public static class FileInfo {
        private final Path path;
        private final Path root;

        private final long size;

        public FileInfo(Path root, Path path) {
            Objects.requireNonNull(root, "Root path is required");
            Objects.requireNonNull(path, "File path is required");

            this.root = root;
            this.path = path;

            File file = path.toFile();

            if (!file.exists() || !file.isFile()) {
                throw new ComponentExecutionException("Path does not pint to valid file");
            }

            size = file.length();
        }

        public String getName() {
            return String.valueOf(path.getFileName());
        }

        public String getRelativePath() {
            return String.valueOf(root.relativize(path));
        }

        public long getSize() {
            return size;
        }
    }
}
