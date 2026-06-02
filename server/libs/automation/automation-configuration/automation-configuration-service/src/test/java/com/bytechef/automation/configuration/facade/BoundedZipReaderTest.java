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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class BoundedZipReaderTest {

    @Test
    void testReadReturnsEntries() throws IOException {
        byte[] zip = zip(Map.of(
            "project.json", "{\"name\":\"Test\"}".getBytes(StandardCharsets.UTF_8),
            "workflow-1.json", "{\"label\":\"Flow\"}".getBytes(StandardCharsets.UTF_8)));

        Map<String, byte[]> entries = new LinkedHashMap<>();

        BoundedZipReader.read(zip, entries::put);

        assertThat(entries).containsOnlyKeys("project.json", "workflow-1.json");
        assertThat(new String(entries.get("project.json"), StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"Test\"}");
    }

    @Test
    void testReadRejectsOversizedEntry() throws IOException {
        byte[] payload = new byte[(int) BoundedZipReader.MAX_ENTRY_SIZE + 1];

        Arrays.fill(payload, (byte) 'A');

        byte[] zip = zip(Map.of("workflow-1.json", payload));

        assertThatThrownBy(() -> BoundedZipReader.read(zip, (name, data) -> {}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uncompressed size");
    }

    @Test
    void testReadRejectsExcessiveTotalSize() throws IOException {
        byte[] payload = new byte[(int) BoundedZipReader.MAX_ENTRY_SIZE];

        Arrays.fill(payload, (byte) 'A');

        Map<String, byte[]> files = new LinkedHashMap<>();

        long entries = BoundedZipReader.MAX_TOTAL_SIZE / BoundedZipReader.MAX_ENTRY_SIZE + 2;

        for (int i = 0; i < entries; i++) {
            files.put("workflow-" + i + ".json", payload);
        }

        byte[] zip = zip(files);

        assertThatThrownBy(() -> BoundedZipReader.read(zip, (name, data) -> {}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uncompressed size");
    }

    @Test
    void testReadRejectsTooManyEntries() throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();

        for (int i = 0; i <= BoundedZipReader.MAX_ENTRY_COUNT; i++) {
            files.put("workflow-" + i + ".json", new byte[] {
                'x'
            });
        }

        byte[] zip = zip(files);

        assertThatThrownBy(() -> BoundedZipReader.read(zip, (name, data) -> {}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entry count");
    }

    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));

                zipOutputStream.write(entry.getValue());

                zipOutputStream.closeEntry();
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}
