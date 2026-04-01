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

package com.bytechef.component.resend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Encoder;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.FileEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class ResendUtilsTest {

    private final ArgumentCaptor<byte[]> bytesArgumentCaptor = forClass(byte[].class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Executor>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testGetAttachments() throws IOException {
        byte[] bytes = {};

        when(mockedFileEntry.getName())
            .thenReturn("fileName");

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, Executor> value = fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });
        when(mockedFile.readAllBytes(fileEntryArgumentCaptor.capture()))
            .thenReturn(bytes);

        when(mockedContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, Executor> value = encoderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64Encode(bytesArgumentCaptor.capture()))
            .thenReturn("encoded");

        List<Map<String, String>> result = ResendUtils.getAttachments(List.of(mockedFileEntry), mockedContext);

        assertEquals(List.of(Map.of("filename", "fileName", "content", "encoded")), result);
        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(encoderFunctionArgumentCaptor.getValue());
        assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
        assertEquals(bytes, bytesArgumentCaptor.getValue());
    }
}
