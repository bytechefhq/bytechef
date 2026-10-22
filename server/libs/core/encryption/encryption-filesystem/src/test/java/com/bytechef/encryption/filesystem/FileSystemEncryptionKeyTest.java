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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileSystemEncryptionKeyTest {

    private static final int THREAD_COUNT = 16;

    private String originalUserHome;

    @BeforeEach
    public void beforeEach() {
        originalUserHome = System.getProperty("user.home");
    }

    @AfterEach
    public void afterEach() {
        System.setProperty("user.home", originalUserHome);
    }

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
    public void testGetKeyCreatesKeyFile() throws IOException {
        Path userHomePath = Files.createTempDirectory("user.home");

        System.setProperty("user.home", userHomePath.toFile()
            .getAbsolutePath());

        FileSystemEncryptionKey encryptionKey = new FileSystemEncryptionKey();

        Path bytechefPath = userHomePath.resolve(".bytechef");

        Assertions.assertFalse(encryptionKey.getKey()
            .isBlank());
        Assertions.assertEquals(encryptionKey.getKey(), Files.readString(bytechefPath.resolve("key")));
        Assertions.assertEquals(List.of("key"), listFileNames(bytechefPath));
    }

    @Test
    public void testConcurrentConstructionAgreesOnOneKey() throws Exception {
        Path userHomePath = Files.createTempDirectory("user.home");

        System.setProperty("user.home", userHomePath.toFile()
            .getAbsolutePath());

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < THREAD_COUNT; index++) {
                futures.add(executorService.submit(() -> {
                    startLatch.await();

                    return new FileSystemEncryptionKey().getKey();
                }));
            }

            startLatch.countDown();

            Set<String> keys = new HashSet<>();

            for (Future<String> future : futures) {
                keys.add(future.get(30, TimeUnit.SECONDS));
            }

            Path bytechefPath = userHomePath.resolve(".bytechef");

            Assertions.assertEquals(Set.of(Files.readString(bytechefPath.resolve("key"))), keys);
            Assertions.assertEquals(List.of("key"), listFileNames(bytechefPath));
        } finally {
            executorService.shutdownNow();
        }
    }

    private static List<String> listFileNames(Path directoryPath) throws IOException {
        try (Stream<Path> paths = Files.list(directoryPath)) {
            return paths.map(path -> String.valueOf(path.getFileName()))
                .toList();
        }
    }
}
