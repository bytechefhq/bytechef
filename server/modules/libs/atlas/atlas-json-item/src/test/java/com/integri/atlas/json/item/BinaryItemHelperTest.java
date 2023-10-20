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

package com.integri.atlas.json.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
public class BinaryItemHelperTest {

    private static final StorageService storageService = Mockito.mock(StorageService.class);

    @Test
    public void testWriteBinaryData() {
        when(storageService.write(anyString(), anyString(), Mockito.any())).thenReturn("/tmp/sample.txt");

        BinaryItemHelper binaryItemHelper = new BinaryItemHelper(storageService);

        assertThat(binaryItemHelper.writeBinaryData("/tmp/sample.txt", new ByteArrayInputStream("data".getBytes())))
            .hasFieldOrPropertyWithValue("data", "/tmp/sample.txt")
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt");

        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(storageService).write(anyString(), fileNameArgumentCaptor.capture(), any(InputStream.class));

        assertThat(fileNameArgumentCaptor.getValue()).endsWith("_sample.txt");
    }

    @Test
    public void testOpenDataInputStream() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        when(storageService.openInputStream(anyString(), anyString())).thenReturn(inputStream);

        BinaryItemHelper binaryItemHelper = new BinaryItemHelper(storageService);

        assertThat(binaryItemHelper.openDataInputStream(BinaryItem.of("sample.txt", "data"))).isEqualTo(inputStream);

        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(storageService).openInputStream(anyString(), fileNameArgumentCaptor.capture());

        assertThat(fileNameArgumentCaptor.getValue()).isEqualTo("data");
    }
}
