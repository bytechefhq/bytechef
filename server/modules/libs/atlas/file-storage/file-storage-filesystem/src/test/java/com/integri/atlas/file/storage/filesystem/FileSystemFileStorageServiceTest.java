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

package com.integri.atlas.file.storage.filesystem;

import com.integri.atlas.file.storage.FileEntry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileSystemFileStorageServiceTest {

    private static final String TEST_STRING = "test string";

    private static final FileSystemFileStorageService fileStorageService = new FileSystemFileStorageService(
        "/tmp/integri/files"
    );

    @Test
    public void testOpenInputStream() throws IOException {
        FileEntry fileEntry = fileStorageService.storeFile(
            "fileName.txt",
            new ByteArrayInputStream(TEST_STRING.getBytes())
        );

        InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());

        Assertions.assertThat(new String(inputStream.readAllBytes())).isEqualTo(TEST_STRING);
    }

    @Test
    public void testRead() {
        FileEntry fileEntry = fileStorageService.storeFile(
            "fileName.txt",
            new ByteArrayInputStream(TEST_STRING.getBytes())
        );

        Assertions.assertThat(fileStorageService.readFileContent(fileEntry.getUrl())).isEqualTo(TEST_STRING);
    }

    @Test
    public void testWrite() {
        FileEntry fileEntry = fileStorageService.storeFile(
            "fileName.txt",
            new ByteArrayInputStream(TEST_STRING.getBytes())
        );

        String path = fileEntry.getUrl();

        Assertions.assertThat(path).startsWith("file:/tmp/integri/files/");

        String url = fileEntry.getUrl();

        Assertions
            .assertThat(Files.contentOf(new File(url.replace("file:", "")), Charset.defaultCharset()))
            .isEqualTo(TEST_STRING);
    }
}
