
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

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.filesystem.action.FilesystemLsAction;
import com.bytechef.component.filesystem.action.FilesystemMkdirAction;
import com.bytechef.component.filesystem.action.FilesystemReadFileAction;
import com.bytechef.component.filesystem.action.FilesystemRmAction;
import com.bytechef.component.filesystem.action.FilesystemWriteFileAction;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class FilesystemComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);
    private static final FilesystemComponentHandler filesystemComponentHandler = new FilesystemComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/filesystem_v1.json", new FilesystemComponentHandler().getDefinition());
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
    public void testPerformReadFile() {
        Parameters parameters = Mockito.mock(Parameters.class);
        File file = getSampleFile();

        Mockito.when(parameters.getRequiredString(FILENAME))
            .thenReturn(file.getAbsolutePath());

        FilesystemReadFileAction.performReadFile(context, parameters);

        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(file.getAbsolutePath());
    }

    @Test
    public void testPerformWriteFile() throws IOException {
        Parameters parameters = Mockito.mock(Parameters.class);
        File file = getSampleFile();

        Mockito.when(parameters.get(FILE_ENTRY, Context.FileEntry.class))
            .thenReturn(Mockito.mock(Context.FileEntry.class));
        Mockito.when(parameters.getRequiredString(FILENAME))
            .thenReturn(file.getAbsolutePath());

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        assertThat(FilesystemWriteFileAction.performWriteFile(context, parameters))
            .hasFieldOrPropertyWithValue("bytes", 5L);
    }

    @Test
    public void testList1() {
        Parameters parameters = Mockito.mock(Parameters.class);
        File file = getLsFile();

        Mockito.when(parameters.getRequiredString("path"))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(parameters.getBoolean("recursive", false))
            .thenReturn(true);

        List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.performLs(context,
            parameters);

        Assertions.assertEquals(
            Set.of("C.txt", "B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getName)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testList2() {
        Parameters parameters = Mockito.mock(Parameters.class);
        File file = getLsFile();

        Mockito.when(parameters.getRequiredString("path"))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(parameters.getBoolean("recursive", false))
            .thenReturn(true);

        List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.performLs(context,
            parameters);

        Assertions.assertEquals(
            Set.of("sub1/C.txt", "B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getRelativePath)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testList3() {
        Parameters parameters = Mockito.mock(Parameters.class);
        File file = getLsFile();

        Mockito.when(parameters.getRequiredString("path"))
            .thenReturn(file.getAbsolutePath());

        List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.performLs(context,
            parameters);

        Assertions.assertEquals(
            Set.of("B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getName)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testCreateDir1() {
        Parameters parameters = Mockito.mock(Parameters.class);
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID()
            .toString()
            .replace("-", "");

        Mockito.when(parameters.getRequiredString("path"))
            .thenReturn(tempDir);

        FilesystemMkdirAction.performMkdir(context, parameters);

        Assertions.assertTrue(new File(tempDir).exists());
    }

    @Test
    public void testCreateDir2() {
        Assertions.assertThrows(ActionExecutionException.class, () -> {
            Parameters parameters = Mockito.mock(Parameters.class);

            Mockito.when(parameters.getRequiredString("path"))
                .thenReturn("/no/such/thing");

            FilesystemMkdirAction.performMkdir(context, parameters);
        });
    }

    @Test
    public void testDelete() throws IOException {
        File tempDir = java.nio.file.Files.createTempDirectory("rm_")
            .toFile();

        Assertions.assertTrue(tempDir.exists());

        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredString("path"))
            .thenReturn(tempDir.getAbsolutePath());

        FilesystemRmAction.performRm(context, parameters);

        Assertions.assertFalse(tempDir.exists());
    }

    private static File getLsFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/ls")
            .getFile());
    }

    private File getSampleFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/sample.txt")
            .getFile());
    }
}
