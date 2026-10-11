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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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

    @Test
    public void testReadOrCreateKeyCreatesTheKeyFileWhenMissing() throws Exception {
        Path keyDirectoryPath = Files.createTempDirectory("bytechef");

        String key = FileSystemEncryptionKey.readOrCreateKey(keyDirectoryPath, () -> "new-key");

        Assertions.assertEquals("new-key", key);
        Assertions.assertEquals("new-key", Files.readString(keyDirectoryPath.resolve("key")));
        Assertions.assertEquals(List.of(keyDirectoryPath.resolve("key")), listFiles(keyDirectoryPath));
    }

    @Test
    public void testReadOrCreateKeyKeepsAnExistingKey() throws Exception {
        Path keyDirectoryPath = Files.createTempDirectory("bytechef");

        Files.writeString(keyDirectoryPath.resolve("key"), "existing-key");

        Assertions.assertEquals(
            "existing-key", FileSystemEncryptionKey.readOrCreateKey(keyDirectoryPath, () -> "new-key"));
    }

    @Test
    public void testConcurrentReadOrCreateKeyAgreesOnOneKey() throws Exception {
        int threadCount = 8;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            for (int round = 0; round < 50; round++) {
                Path keyDirectoryPath = Files.createTempDirectory("bytechef");
                CountDownLatch startLatch = new CountDownLatch(1);
                List<Future<String>> keyFutures = new ArrayList<>();

                for (int thread = 0; thread < threadCount; thread++) {
                    keyFutures.add(executorService.submit(() -> {
                        Assertions.assertTrue(startLatch.await(10, TimeUnit.SECONDS));

                        return FileSystemEncryptionKey.readOrCreateKey(
                            keyDirectoryPath, () -> UUID.randomUUID()
                                .toString());
                    }));
                }

                startLatch.countDown();

                String publishedKey = null;

                for (Future<String> keyFuture : keyFutures) {
                    String key = keyFuture.get(10, TimeUnit.SECONDS);

                    Assertions.assertFalse(key.isEmpty());

                    if (publishedKey == null) {
                        publishedKey = key;
                    }

                    Assertions.assertEquals(publishedKey, key);
                }

                Assertions.assertEquals(publishedKey, Files.readString(keyDirectoryPath.resolve("key")));
                Assertions.assertEquals(List.of(keyDirectoryPath.resolve("key")), listFiles(keyDirectoryPath));
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private static List<Path> listFiles(Path directoryPath) throws IOException {
        try (Stream<Path> paths = Files.list(directoryPath)) {
            return paths.toList();
        }
    }
}
