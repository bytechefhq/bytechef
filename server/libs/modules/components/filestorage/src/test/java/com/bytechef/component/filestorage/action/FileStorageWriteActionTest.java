
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

package com.bytechef.component.filestorage.action;

import com.bytechef.component.filestorage.FileStorageComponentHandlerTest;
import com.bytechef.hermes.component.Context;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILENAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class FileStorageWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testExecuteWrite() {
        File file = getFile();

        Map<String, ?> inputParameters = Map.of(
            CONTENT, Files.contentOf(file, StandardCharsets.UTF_8),
            FILENAME, "file.txt");

        FileStorageWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue()).isEqualTo(Files.contentOf(file, StandardCharsets.UTF_8));
        assertThat(filenameArgumentCaptor.getValue()).isEqualTo("file.txt");

        inputParameters = Map.of(
            CONTENT, Files.contentOf(file, StandardCharsets.UTF_8),
            FILENAME, "test.txt");

        Mockito.reset(context);

        FileStorageWriteAction.executeWrite(context, inputParameters);

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
