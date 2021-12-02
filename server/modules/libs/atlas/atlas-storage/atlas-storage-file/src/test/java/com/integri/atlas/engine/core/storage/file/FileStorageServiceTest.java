/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
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
    public void testWrite() {
        Assertions
            .assertThat(fileStorageService.write("binary", "file.txt", new ByteArrayInputStream(string.getBytes())))
            .isEqualTo("/tmp/integri/binary/file.txt");

        Assertions
            .assertThat(Files.contentOf(new File("/tmp/integri/binary/file.txt"), Charset.defaultCharset()))
            .isEqualTo(string);
    }

    @Test
    public void testOpenInputStream() throws IOException {
        fileStorageService.write("binary", "file.txt", new ByteArrayInputStream(string.getBytes()));

        InputStream inputStream = fileStorageService.openInputStream("binary", "file.txt");

        Assertions.assertThat(new String(inputStream.readAllBytes())).isEqualTo(string);
    }
}
