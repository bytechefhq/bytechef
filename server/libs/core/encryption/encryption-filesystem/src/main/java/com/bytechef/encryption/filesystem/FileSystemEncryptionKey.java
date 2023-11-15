/*
 * Copyright 2023-present ByteChef Inc.
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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Ivica Cardic
 */
public class FileSystemEncryptionKey extends AbstractEncryptionKey {

    private final String key;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public FileSystemEncryptionKey() {
        Path userHome = Path.of(System.getProperty("user.home"));

        try {
            Path bytechefPath = Files.createDirectories(userHome.resolve(".bytechef"));

            Path keyPath = bytechefPath.resolve("key");

            if (Files.exists(keyPath)) {
                key = Files.readString(keyPath);
            } else {
                key = generateKey();

                Files.writeString(Files.createFile(keyPath), key);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String fetchKey() {
        return key;
    }
}
