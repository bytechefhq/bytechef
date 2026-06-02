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

package com.bytechef.automation.configuration.facade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads ZIP archive entries while enforcing hard limits on entry count and uncompressed size, guarding against
 * decompression (zip) bombs.
 *
 * @author Ivica Cardic
 */
final class BoundedZipReader {

    static final int MAX_ENTRY_COUNT = 1_000;
    static final long MAX_ENTRY_SIZE = 10L * 1024 * 1024;
    static final long MAX_TOTAL_SIZE = 50L * 1024 * 1024;

    private BoundedZipReader() {
    }

    static void read(byte[] data, BiConsumer<String, byte[]> entryConsumer) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry zipEntry;
            int entryCount = 0;
            long totalSize = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ENTRY_COUNT) {
                    throw new IllegalArgumentException(
                        "ZIP archive exceeds the maximum allowed entry count of " + MAX_ENTRY_COUNT);
                }

                byte[] entryData = readEntry(zipInputStream, Math.min(MAX_ENTRY_SIZE, MAX_TOTAL_SIZE - totalSize));

                totalSize += entryData.length;

                entryConsumer.accept(zipEntry.getName(), entryData);

                zipInputStream.closeEntry();
            }
        }
    }

    private static byte[] readEntry(ZipInputStream zipInputStream, long limit) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = zipInputStream.read(buffer)) != -1) {
            total += read;

            if (total > limit) {
                throw new IllegalArgumentException(
                    "ZIP archive exceeds the maximum allowed uncompressed size");
            }

            byteArrayOutputStream.write(buffer, 0, read);
        }

        return byteArrayOutputStream.toByteArray();
    }
}
