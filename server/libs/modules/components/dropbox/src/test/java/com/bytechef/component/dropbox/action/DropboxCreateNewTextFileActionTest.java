/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.dropbox.util.DropboxUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class DropboxCreateNewTextFileActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor = forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILENAME, "file", TEXT, "file content"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() throws IOException {
        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<File, ?> function = invocation.getArgument(0);

                return function.apply(mockedFile);
            });
        when(mockedFile.storeContent(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedFileEntry);

        try (MockedStatic<DropboxUtils> dropboxUtilsMockedStatic = mockStatic(DropboxUtils.class)) {
            dropboxUtilsMockedStatic.when(() -> DropboxUtils.uploadFile(
                parametersArgumentCaptor.capture(), contextArgumentCaptor.capture(), fileEntryArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result = DropboxCreateNewTextFileAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertNotNull(fileFunctionArgumentCaptor.getValue());
            assertEquals(List.of("file.paper", "file content"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
        }
    }
}
