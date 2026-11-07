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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Ivica Cardic
 */
final class BoundedZipReader {

    private final int maxEntryCount;
    private final long maxEntrySize;
    private final long maxTotalSize;

    BoundedZipReader(int maxEntryCount, long maxEntrySize, long maxTotalSize) {
        this.maxEntryCount = maxEntryCount;
        this.maxEntrySize = maxEntrySize;
        this.maxTotalSize = maxTotalSize;
    }

    List<Entry> read(byte[] data) throws IOException {
        List<Entry> entries = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry zipEntry;
            long totalSize = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (maxEntryCount > 0 && entries.size() >= maxEntryCount) {
                    throw new IllegalArgumentException(
                        "ZIP archive exceeds the maximum allowed entry count of " + maxEntryCount);
                }

                byte[] entryData = readEntry(zipInputStream, totalSize);

                totalSize += entryData.length;

                entries.add(new Entry(zipEntry.getName(), entryData));

                zipInputStream.closeEntry();
            }
        }

        return entries;
    }

    private byte[] readEntry(ZipInputStream zipInputStream, long totalSize) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long entrySize = 0;
        int read;

        while ((read = zipInputStream.read(buffer)) != -1) {
            entrySize += read;

            if (maxEntrySize > 0 && entrySize > maxEntrySize) {
                throw new IllegalArgumentException(
                    "ZIP archive entry exceeds the maximum allowed uncompressed size of " + maxEntrySize + " bytes");
            }

            if (maxTotalSize > 0 && totalSize + entrySize > maxTotalSize) {
                throw new IllegalArgumentException(
                    "ZIP archive exceeds the maximum allowed total uncompressed size of " + maxTotalSize + " bytes");
            }

            byteArrayOutputStream.write(buffer, 0, read);
        }

        return byteArrayOutputStream.toByteArray();
    }

    record Entry(String name, byte[] data) {
    }
}
