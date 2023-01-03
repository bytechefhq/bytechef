
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

package com.bytechef.component.filesystem;

import static com.bytechef.component.filesystem.constants.FilesystemConstants.CREATE_TEMP_DIR;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.FILESYSTEM;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.FILE_ENTRY;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.GET_FILE_PATH;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.LS;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.MKDIR;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.READ_FILE;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.RM;
import static com.bytechef.component.filesystem.constants.FilesystemConstants.WRITE_FILE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class FilesystemComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(FILESYSTEM)
        .display(display("Local File").description("Reads or writes a binary file from/to disk"))
        .actions(
            action(READ_FILE)
                .display(display("Read from file"))
                .properties(string(FILENAME)
                    .label("Filename")
                    .description("The path of the file to read.")
                    .placeholder("/data/your_file.pdf")
                    .required(true))
                .output(ComponentDSL.fileEntry())
                .perform(this::performReadFile),
            action(WRITE_FILE)
                .display(display("Write to file"))
                .properties(
                    fileEntry(FILE_ENTRY)
                        .label("File")
                        .description(
                            "The object property which contains a reference to the file to be written.")
                        .required(true),
                    string(FILENAME)
                        .label("Filename")
                        .description("The path to which the file should be written.")
                        .placeholder("/data/your_file.pdf")
                        .required(true))
                .output(object().properties(integer("bytes")))
                .perform(this::performWriteFile),
            action(CREATE_TEMP_DIR)
                .display(display("Create Temp Directory")
                    .description("Creates a temporary directory oon the filesystem."))
                .perform(this::performCreateTempDir),
            action(GET_FILE_PATH)
                .display(
                    display("File Path")
                        .description(
                            "Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory separator."))
                .properties(string(FILENAME)
                    .label("Filename")
                    .description("The path to full filename.")
                    .placeholder("/data/your_file.pdf")
                    .required(true))
                .perform(this::performGetFilePath)
                .output(string())
                .exampleOutput("/data"),
            action(LS)
                .display(display("Lists a content of directory for the given path.")
                    .description("")),
            action(MKDIR)
                .display(display("Create").description("Creates a directory."))
                .perform(this::performCreateDir),
            action(RM)
                .display(display("Remove").description("Removes the content of a directory."))
                .perform(this::performDelete));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     *
     * <p>
     * An exception is not thrown if the directory could not be created because it already exists.
     */
    protected Object performCreateDir(Context context, ExecutionParameters executionParameters) {
        try {
            return Files.createDirectories(Paths.get(executionParameters.getRequiredString("path")));
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to create directories " + executionParameters, ioException);
        }
    }

    protected String performCreateTempDir(Context context, ExecutionParameters executionParameters) {
        try {
            Path path = Files.createTempDirectory("createTempDir_");

            File file = path.toFile();

            return file.getAbsolutePath();
        } catch (IOException ioException) {
            throw new ActionExecutionException(
                "Unable to create temporary directory " + executionParameters, ioException);
        }
    }

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all subdirectories.
     *
     * <p>
     * A directory to be deleted does not have to be empty.
     * </p>
     */
    protected Object performDelete(Context context, ExecutionParameters executionParameters) {
        File file = new File(executionParameters.getRequiredString("path"));

        try {
            return deleteRecursively(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory
     * separator.
     *
     * <p>
     * This method will handle a file in either Unix or Windows format. The method is entirely text based and returns
     * the text before the last forward or backslash.
     */
    protected String performGetFilePath(Context context, ExecutionParameters executionParameters) {
        String filename = executionParameters.getRequiredString("filename");

        return filename.substring(0, filename.lastIndexOf(File.separator));
    }

    protected List<FileInfo> performList(Context context, ExecutionParameters executionParameters) {
        Path root = Paths.get(executionParameters.getRequiredString("path"));

        boolean recursive = executionParameters.getBoolean("recursive", false);

        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> recursive || p.getParent()
                .equals(root))
                .filter(Files::isRegularFile)
                .map(p -> new FileInfo(root, p))
                .collect(Collectors.toList());
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to list directory entries", ioException);
        }
    }

    protected FileEntry performReadFile(Context context, ExecutionParameters executionParameters) {
        String filename = executionParameters.getRequiredString(FILENAME);

        try (InputStream inputStream = new FileInputStream(filename)) {
            return context.storeFileContent(filename, inputStream);
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to open file " + executionParameters, ioException);
        }
    }

    protected Map<String, Long> performWriteFile(Context context, ExecutionParameters executionParameters) {
        String fileName = executionParameters.getRequiredString(FILENAME);

        try (InputStream inputStream = context.getFileStream(executionParameters.get(FILE_ENTRY, FileEntry.class))) {
            return Map.of("bytes", Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING));
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to create file " + executionParameters, ioException);
        }
    }

    protected static class FileInfo {
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

    private boolean deleteRecursively(Path root) throws IOException {
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
