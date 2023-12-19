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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.file.storage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILENAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.file.storage.FileStorageComponentHandlerTest;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
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
@Disabled
public class FileStorageWriteActionTest {

    private static final ActionContext context = Mockito.mock(ActionContext.class);

    @Test
    public void testPerformWrite() {
        File file = getFile();
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequired(Mockito.eq(CONTENT)))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));
        Mockito.when(parameterMap.getString(Mockito.eq(FILENAME), Mockito.eq("file.txt")))
            .thenReturn("file.txt");

        FileStorageWriteAction.perform(parameterMap, parameterMap, context);

        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(context)
            .file(
                file1 -> file1.storeContent(filenameArgumentCaptor.capture(), contentArgumentCaptor.capture()));

        assertThat(contentArgumentCaptor.getValue()).isEqualTo(Files.contentOf(file, StandardCharsets.UTF_8));
        assertThat(filenameArgumentCaptor.getValue()).isEqualTo("file.txt");

        Mockito.reset(context);
        Mockito.reset(parameterMap);

        Mockito.when(parameterMap.getRequired(Mockito.eq(CONTENT)))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));
        Mockito.when(parameterMap.getString(Mockito.eq(FILENAME), Mockito.eq("file.txt")))
            .thenReturn("test.txt");

        FileStorageWriteAction.perform(parameterMap, parameterMap, context);

        Mockito.verify(context)
            .file(file1 -> file1.storeContent(filenameArgumentCaptor.capture(), Mockito.anyString()));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo("test.txt");
    }

    private File getFile() {
        return new File(FileStorageComponentHandlerTest.class
            .getClassLoader()
            .getResource("file-storage/dependencies/sample.txt")
            .getFile());
    }
}
