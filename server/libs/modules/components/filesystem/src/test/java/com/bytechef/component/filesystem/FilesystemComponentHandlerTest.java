
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
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
    public void testExecuteCreateDir() {
        // TODO
    }

    @Disabled
    @Test
    public void testExecuteCreateTempDir() {
        // TODO
    }

    @Disabled
    @Test
    public void testExecuteDelete() {
        // TODO
    }

    @Disabled
    @Test
    public void testExecuteDownload() {
        // TODO
    }

    @Disabled
    @Test
    public void testExecuteGetFilePath() {
        // TODO
    }

    @Disabled
    @Test
    public void testExecuteList() {
        // TODO
    }

    @Test
    public void testExecuteReadFile() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);
        File file = getSampleFile();

        Mockito.when(inputParameters.getRequiredString(FILENAME))
            .thenReturn(file.getAbsolutePath());

        FilesystemReadFileAction.executeReadFile(context, inputParameters);

        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(file.getAbsolutePath());
    }

    @Test
    public void testExecuteWriteFile() throws IOException {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);
        File file = getSampleFile();

        Mockito.when(inputParameters.get(FILE_ENTRY, Context.FileEntry.class))
            .thenReturn(Mockito.mock(Context.FileEntry.class));
        Mockito.when(inputParameters.getRequiredString(FILENAME))
            .thenReturn(file.getAbsolutePath());

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        assertThat(FilesystemWriteFileAction.executeWriteFile(context, inputParameters))
            .hasFieldOrPropertyWithValue("bytes", 5L);
    }

    @Test
    public void testList1() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);
        File file = getLsFile();

        Mockito.when(inputParameters.getRequiredString("path"))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(inputParameters.getBoolean("recursive", false))
            .thenReturn(true);

        List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.executeLs(context,
            inputParameters);

        Assertions.assertEquals(
            Set.of("C.txt", "B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getName)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testList2() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);
        File file = getLsFile();

        Mockito.when(inputParameters.getRequiredString("path"))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(inputParameters.getBoolean("recursive", false))
            .thenReturn(true);

        List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.executeLs(context,
            inputParameters);

        Assertions.assertEquals(
            Set.of("sub1/C.txt", "B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getRelativePath)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testList3() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);
        File file = getLsFile();

        Mockito.when(inputParameters.getRequiredString("path"))
            .thenReturn(file.getAbsolutePath());

        List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.executeLs(context, inputParameters);

        Assertions.assertEquals(
            Set.of("B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getName)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testCreateDir1() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID()
            .toString()
            .replace("-", "");

        Mockito.when(inputParameters.getRequiredString("path"))
            .thenReturn(tempDir);

        FilesystemMkdirAction.executeMkdir(context, inputParameters);

        Assertions.assertTrue(new File(tempDir).exists());
    }

    @Test
    public void testCreateDir2() {
        Assertions.assertThrows(ComponentExecutionException.class, () -> {
            InputParameters inputParameters = Mockito.mock(InputParameters.class);

            Mockito.when(inputParameters.getRequiredString("path"))
                .thenReturn("/no/such/thing");

            FilesystemMkdirAction.executeMkdir(context, inputParameters);
        });
    }

    @Test
    public void testDelete() throws IOException {
        File tempDir = java.nio.file.Files.createTempDirectory("rm_")
            .toFile();

        Assertions.assertTrue(tempDir.exists());

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequiredString("path"))
            .thenReturn(tempDir.getAbsolutePath());

        FilesystemRmAction.executeRm(context, inputParameters);

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
