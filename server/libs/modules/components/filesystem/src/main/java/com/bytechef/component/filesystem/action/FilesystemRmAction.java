
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
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.RM;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;

/**
 * @author Ivica Cardic
 */
public class FilesystemRmAction {

    public static final ActionDefinition RM_ACTION = action(RM)
        .display(display("Remove").description("Removes the content of a directory."))
        .perform(FilesystemRmAction::performRm);

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all subdirectories.
     *
     * <p>
     * A directory to be deleted does not have to be empty.
     * </p>
     */
    public static Object performRm(Context context, ExecutionParameters executionParameters) {
        File file = new File(executionParameters.getRequiredString("path"));

        try {
            return deleteRecursively(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return false;
        }

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);

                return FileVisitResult.CONTINUE;
            }
        });

        return true;
    }
}
