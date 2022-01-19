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

package com.integri.atlas.engine.core.storage.file;

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
public class FileStorageServiceTest {

    private static final String string = "test string";

    private static final FileStorageService fileStorageService = new FileStorageService("/tmp/integri");

    @Test
    public void testOpenInputStream() throws IOException {
        String fileName = fileStorageService.write("binary", new ByteArrayInputStream(string.getBytes()));

        InputStream inputStream = fileStorageService.openInputStream("binary", fileName);

        Assertions.assertThat(new String(inputStream.readAllBytes())).isEqualTo(string);
    }

    @Test
    public void testRead() {
        String fileName = fileStorageService.write("binary", new ByteArrayInputStream(string.getBytes()));

        Assertions.assertThat(fileStorageService.read("binary", fileName)).isEqualTo(string);
    }

    @Test
    public void testWrite() {
        String fileName = fileStorageService.write("binary", new ByteArrayInputStream(string.getBytes()));

        Assertions.assertThat(fileName).startsWith("/tmp/integri/binary/");

        Assertions.assertThat(Files.contentOf(new File(fileName), Charset.defaultCharset())).isEqualTo(string);
    }
}
