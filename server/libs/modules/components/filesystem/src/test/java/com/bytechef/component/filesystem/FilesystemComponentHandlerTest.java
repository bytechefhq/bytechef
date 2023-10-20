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

import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class FilesystemComponentHandlerTest {

    private static final Context context = new MockContext();
    private static final FilesystemComponentHandler filesystemComponentHandler = new FilesystemComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        AssertUtils.assertEquals("definition/filesystem_v1.json", new FilesystemComponentHandler().getDefinition());
    }

    @Disabled
    @Test
    public void testPerformCreateDir() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformCreateTempDir() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformDelete() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformDownload() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformGetFilePath() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformList() {
        // TODO
    }

    @Test
    public void testPerformReadFile() throws IOException {
        File file = getFile();

        ExecutionParameters executionParameters = getParameters(file.getAbsolutePath(), null);

        FileEntry fileEntry = context.storeFileContent(file.getName(), Files.contentOf(file, Charset.defaultCharset()));

        assertThat(filesystemComponentHandler.performReadFile(context, executionParameters))
                .hasFieldOrPropertyWithValue("extension", FilenameUtils.getExtension(file.getAbsolutePath()))
                .hasFieldOrPropertyWithValue("mimeType", "text/plain")
                .hasFieldOrPropertyWithValue("name", FilenameUtils.getName(file.getAbsolutePath()))
                .hasFieldOrPropertyWithValue("url", fileEntry.getUrl());
    }

    @Test
    public void testPerformWriteFile() throws IOException {
        File file = getFile();

        ExecutionParameters executionParameters = getParameters(
                file.getAbsolutePath(), context.storeFileContent(file.getName(), new FileInputStream(file)));

        assertThat(filesystemComponentHandler.performWriteFile(context, executionParameters))
                .hasFieldOrPropertyWithValue("bytes", 5L);
    }

    @Test
    public void testList1() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/ls");
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("path", classPathResource.getFile().getAbsolutePath());
        parameters.set("recursive", true);

        List<FilesystemComponentHandler.FileInfo> files =
                (List<FilesystemComponentHandler.FileInfo>) filesystemComponentHandler.performList(context, parameters);

        Assertions.assertEquals(
                Set.of("C.txt", "B.txt", "A.txt"),
                files.stream().map(FilesystemComponentHandler.FileInfo::getName).collect(Collectors.toSet()));
    }

    @Test
    public void testList2() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/ls");
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("path", classPathResource.getFile().getAbsolutePath());
        parameters.set("recursive", true);

        List<FilesystemComponentHandler.FileInfo> files =
                (List<FilesystemComponentHandler.FileInfo>) filesystemComponentHandler.performList(context, parameters);

        Assertions.assertEquals(
                Set.of("sub1/C.txt", "B.txt", "A.txt"),
                files.stream()
                        .map(FilesystemComponentHandler.FileInfo::getRelativePath)
                        .collect(Collectors.toSet()));
    }

    @Test
    public void testList3() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/ls");
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("path", classPathResource.getFile().getAbsolutePath());

        List<FilesystemComponentHandler.FileInfo> files =
                (List<FilesystemComponentHandler.FileInfo>) filesystemComponentHandler.performList(context, parameters);

        Assertions.assertEquals(
                Set.of("B.txt", "A.txt"),
                files.stream().map(FilesystemComponentHandler.FileInfo::getName).collect(Collectors.toSet()));
    }

    @Test
    public void testCreateDir1() {
        MockExecutionParameters parameters = new MockExecutionParameters();
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + RandomStringUtils.randomAlphabetic(10);

        parameters.set("path", tempDir);

        filesystemComponentHandler.performCreateDir(context, parameters);

        Assertions.assertTrue(new File(tempDir).exists());
    }

    @Test
    public void testCreateDir2() {
        Assertions.assertThrows(ActionExecutionException.class, () -> {
            MockExecutionParameters parameters = new MockExecutionParameters();

            parameters.set("path", "/no/such/thing");

            filesystemComponentHandler.performCreateDir(context, parameters);
        });
    }

    @Test
    public void testDelete() throws IOException {
        MockExecutionParameters parameters = new MockExecutionParameters();
        File tempDir = getTempDir();

        Assertions.assertTrue(tempDir.exists());

        parameters.set("path", tempDir.getAbsolutePath());

        filesystemComponentHandler.performDelete(context, parameters);

        Assertions.assertFalse(tempDir.exists());
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }

    private ExecutionParameters getParameters(String fileName, FileEntry fileEntry) {
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(FILE_ENTRY, fileEntry == null ? null : fileEntry.toMap());
        parameters.set(FILENAME, fileName);

        return parameters;
    }

    private File getTempDir() throws IOException {
        Path path = java.nio.file.Files.createTempDirectory("rm_");

        return path.toFile();
    }
}
