
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

package com.bytechef.component.filestorage;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILENAME;
import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.filestorage.action.FileStorageReadAction;
import com.bytechef.component.filestorage.action.FileStorageWriteAction;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class FileStorageComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);
    private static final FileStorageComponentHandler fileStorageComponentHandler = new FileStorageComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/filestorage_v1.json", fileStorageComponentHandler.getDefinition());
    }

    @Test
    public void testPerformRead() {
        Parameters parameters = Mockito.mock(Parameters.class);

        Context.FileEntry fileEntry = Mockito.mock(Context.FileEntry.class);

        Mockito.when(parameters.get(FILE_ENTRY, Context.FileEntry.class))
            .thenReturn(fileEntry);

        FileStorageReadAction.performRead(context, parameters);

        ArgumentCaptor<Context.FileEntry> fileEntryArgumentCaptor = ArgumentCaptor.forClass(Context.FileEntry.class);

        Mockito.verify(context)
            .readFileToString(fileEntryArgumentCaptor.capture());

        assertThat(fileEntryArgumentCaptor.getValue()).isEqualTo(fileEntry);
    }

    @Disabled
    @Test
    public void testPerformDownload() {
        // TODO
    }

    @Test
    public void testPerformWrite() {
        File file = getFile();

        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequired(CONTENT))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));
        Mockito.when(parameters.getString(FILENAME, "file.txt"))
            .thenReturn("file.txt");

        FileStorageWriteAction.performWrite(context, parameters);

        ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue()).isEqualTo(Files.contentOf(file, StandardCharsets.UTF_8));
        assertThat(filenameArgumentCaptor.getValue()).isEqualTo("file.txt");

        parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequired(CONTENT))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));
        Mockito.when(parameters.getString(FILENAME, "file.txt"))
            .thenReturn("test.txt");

        Mockito.reset(context);

        FileStorageWriteAction.performWrite(context, parameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.anyString());

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo("test.txt");
    }

    private File getFile() {
        return new File(FileStorageComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/sample.txt")
            .getFile());
    }
}
