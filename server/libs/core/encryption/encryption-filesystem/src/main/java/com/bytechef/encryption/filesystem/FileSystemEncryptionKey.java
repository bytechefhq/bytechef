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

import com.bytechef.encryption.AbstractEncryptionKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

/**
 * @author Ivica Cardic
 */
public class FileSystemEncryptionKey extends AbstractEncryptionKey {

    private final String key;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public FileSystemEncryptionKey() {
        Path userHome = Path.of(System.getProperty("user.home"));

        try {
            key = readOrCreateKey(Files.createDirectories(userHome.resolve(".bytechef")), this::generateKey);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    static String readOrCreateKey(Path keyDirectoryPath, Callable<String> keyGenerator) throws Exception {
        Path keyPath = keyDirectoryPath.resolve("key");

        synchronized (FileSystemEncryptionKey.class) {
            try (FileChannel fileChannel = FileChannel.open(
                keyPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {

                FileLock fileLock = fileChannel.lock();

                try {
                    if (fileChannel.size() > 0) {
                        return readKey(fileChannel);
                    }

                    String newKey = keyGenerator.call();

                    fileChannel.write(ByteBuffer.wrap(newKey.getBytes(StandardCharsets.UTF_8)));
                    fileChannel.force(true);

                    return newKey;
                } finally {
                    fileLock.release();
                }
            }
        }
    }

    private static String readKey(FileChannel fileChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());

        while (byteBuffer.hasRemaining()) {
            if (fileChannel.read(byteBuffer, byteBuffer.position()) < 0) {
                break;
            }
        }

        return new String(byteBuffer.array(), 0, byteBuffer.position(), StandardCharsets.UTF_8);
    }

    @Override
    protected String doGetKey() {
        return key;
    }
}
