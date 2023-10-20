/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.core.binary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integri.atlas.engine.core.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class BinaryHelperTest {

    private static final StorageService storageService = Mockito.mock(StorageService.class);

    @Test
    public void testOpenDataInputStream() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        when(storageService.openInputStream(anyString(), anyString())).thenReturn(inputStream);

        BinaryHelper binaryHelper = new BinaryHelper(storageService);

        assertThat(binaryHelper.openDataInputStream(Binary.of("sample.txt", "data"))).isEqualTo(inputStream);

        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(storageService).openInputStream(anyString(), fileNameArgumentCaptor.capture());

        assertThat(fileNameArgumentCaptor.getValue()).isEqualTo("data");
    }

    @Test
    public void testReadBinaryData() {
        String data = "test data";

        when(storageService.read(anyString(), anyString())).thenReturn(data);

        BinaryHelper binaryHelper = new BinaryHelper(storageService);

        assertThat(binaryHelper.readBinaryData(Binary.of("sample.txt", "data"))).isEqualTo(data);

        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(storageService).read(anyString(), fileNameArgumentCaptor.capture());

        assertThat(fileNameArgumentCaptor.getValue()).isEqualTo("data");
    }

    @Test
    public void testWriteBinaryData() {
        when(storageService.write(anyString(), Mockito.any(InputStream.class))).thenReturn("/tmp/sample.txt");

        BinaryHelper binaryHelper = new BinaryHelper(storageService);

        assertThat(binaryHelper.writeBinaryData("/tmp/sample.txt", new ByteArrayInputStream("data".getBytes())))
            .hasFieldOrPropertyWithValue("data", "/tmp/sample.txt")
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt");
    }
}
