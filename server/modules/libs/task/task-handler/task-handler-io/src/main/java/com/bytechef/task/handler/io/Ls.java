/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.task.handler.io;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("io/ls")
class Ls implements TaskHandler<List<Ls.FileInfo>> {

    @Override
    public List<Ls.FileInfo> handle(TaskExecution aTask) throws TaskExecutionException {
        Path root = Paths.get(aTask.getRequiredString("path"));

        boolean recursive = aTask.getBoolean("recursive", false);

        try {
            return Files.walk(root)
                    .filter(p -> recursive || p.getParent().equals(root))
                    .filter(Files::isRegularFile)
                    .map(p -> new FileInfo(root, p))
                    .collect(Collectors.toList());
        } catch (IOException ioException) {
            throw new TaskExecutionException("Unable to list directory entries", ioException);
        }
    }

    public static class FileInfo {

        @NonNull
        private final Path path;

        @NonNull
        private final Path root;

        private final long size;

        public FileInfo(@NonNull Path aRoot, @NonNull Path aPath) {
            Assert.notNull(aRoot, "Root path is required");
            Assert.notNull(aPath, "File path is required");

            root = aRoot;
            path = aPath;

            File file = aPath.toFile();

            if ((file == null) || !file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("Path does not pint to valid file");
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
