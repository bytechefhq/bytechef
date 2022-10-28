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

package com.bytechef.hermes.file.storage.filesystem.service;

import com.bytechef.hermes.file.storage.domain.FileEntry;
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
public class FilesystemFileStorageServiceTest {

    private static final String TEST_STRING = "test string";

    private static final FilesystemFileStorageService fileStorageService =
            new FilesystemFileStorageService("/tmp/test/bytechef/files");

    @Test
    public void testDeleteFile() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
                "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(Charset.defaultCharset())));

        Assertions.assertThat(fileStorageService.readFileToString(fileEntry)).isEqualTo(TEST_STRING);

        fileStorageService.deleteFile(fileEntry);

        Assertions.assertThat(fileStorageService.fileExists(fileEntry)).isFalse();
    }

    @Test
    public void testOpenInputStream() throws IOException {
        FileEntry fileEntry = fileStorageService.storeFileContent(
                "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(Charset.defaultCharset())));

        InputStream inputStream = fileStorageService.getFileStream(fileEntry);

        Assertions.assertThat(new String(inputStream.readAllBytes(), Charset.defaultCharset()))
                .isEqualTo(TEST_STRING);
    }

    @Test
    public void testRead() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
                "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(Charset.defaultCharset())));

        Assertions.assertThat(fileStorageService.readFileToString(fileEntry)).isEqualTo(TEST_STRING);
    }

    @Test
    public void testWrite() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
                "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(Charset.defaultCharset())));

        String path = fileEntry.getUrl();

        Assertions.assertThat(path).startsWith("file:/tmp/test/bytechef/files/");

        String url = fileEntry.getUrl();

        Assertions.assertThat(Files.contentOf(new File(url.replace("file:", "")), Charset.defaultCharset()))
                .isEqualTo(TEST_STRING);
    }
}
