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

package com.bytechef.encryption.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileSystemEncryptionKeyTest {

    @Test
    public void testGetKey() throws IOException {
        Path userHomePath = Files.createTempDirectory("user.home");

        Path keyPath = Files.createDirectories(userHomePath.resolve(".bytechef"))
            .resolve("key");

        Files.writeString(Files.createFile(keyPath), "tTB1/UBIbYLuCXVi4PPfzA==");

        System.setProperty("user.home", userHomePath.toFile()
            .getAbsolutePath());

        FileSystemEncryptionKey encryptionKey = new FileSystemEncryptionKey();

        Assertions.assertEquals("tTB1/UBIbYLuCXVi4PPfzA==", encryptionKey.getKey());
    }
}
