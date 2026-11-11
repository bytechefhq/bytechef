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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.file.storage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.ENCODING;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILENAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileStorageWriteActionTest {

    private static final String CROATIAN = "Šč";

    private final Context context = mock(Context.class);
    private final File file = mock(File.class);
    private final FileEntry fileEntry = mock(FileEntry.class);
    private final AtomicReference<byte[]> storedBytes = new AtomicReference<>();

    @BeforeEach
    void beforeEach() throws Exception {
        when(context.file(any())).thenAnswer(invocation -> {
            ContextFunction<File, ?> function = invocation.getArgument(0);

            return function.apply(file);
        });
        when(file.storeContent(anyString(), anyString())).thenReturn(fileEntry);
        when(file.storeContent(anyString(), any(InputStream.class))).thenAnswer(invocation -> {
            try (InputStream inputStream = invocation.getArgument(1)) {
                storedBytes.set(inputStream.readAllBytes());
            }

            return fileEntry;
        });
    }

    @Test
    void testPerformWritesUtf8ByDefault() throws Exception {
        assertSame(fileEntry, FileStorageWriteAction.perform(parameters(CROATIAN, null), null, context));

        verify(file).storeContent("hnb.xml", CROATIAN);
        verify(file, never()).storeContent(anyString(), any(InputStream.class));
    }

    @Test
    void testPerformWritesUtf8WhenChosen() throws Exception {
        assertSame(fileEntry, FileStorageWriteAction.perform(parameters(CROATIAN, "UTF-8"), null, context));

        verify(file).storeContent("hnb.xml", CROATIAN);
    }

    @Test
    void testPerformWritesTheChosenEncoding() throws Exception {
        assertSame(fileEntry, FileStorageWriteAction.perform(parameters(CROATIAN, "ISO-8859-2"), null, context));
        assertArrayEquals(new byte[] {
            (byte) 0xA9, (byte) 0xE8
        }, storedBytes.get());

        assertSame(fileEntry, FileStorageWriteAction.perform(parameters(CROATIAN, "windows-1250"), null, context));
        assertArrayEquals(new byte[] {
            (byte) 0x8A, (byte) 0xE8
        }, storedBytes.get());

        verify(file, never()).storeContent(anyString(), anyString());
    }

    @Test
    void testPerformWritesANonStringContentAsItsText() throws Exception {
        FileStorageWriteAction.perform(parameters(List.of("č"), "ISO-8859-2"), null, context);

        assertArrayEquals(new byte[] {
            '[', (byte) 0xE8, ']'
        }, storedBytes.get());
    }

    @Test
    void testPerformRejectsACharacterTheEncodingCannotRepresent() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> FileStorageWriteAction.perform(parameters(CROATIAN, "ISO-8859-1"), null, context));

        assertEquals(
            "The content has a character that cannot be encoded in ISO-8859-1",
            illegalArgumentException.getMessage());
    }

    private static Parameters parameters(Object content, String encoding) {
        Map<String, Object> map = new HashMap<>(Map.of(CONTENT, content, FILENAME, "hnb.xml"));

        if (encoding != null) {
            map.put(ENCODING, encoding);
        }

        return MockParametersFactory.create(map);
    }
}
