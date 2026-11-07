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
import java.util.LinkedHashMap;
import java.util.List;
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
        BoundedZipReader boundedZipReader = new BoundedZipReader(10, 1024, 4096);

        Map<String, byte[]> files = new LinkedHashMap<>();

        files.put("project.json", "{\"name\":\"Test\"}".getBytes(StandardCharsets.UTF_8));
        files.put("workflow-1.json", "{\"label\":\"Flow\"}".getBytes(StandardCharsets.UTF_8));

        List<BoundedZipReader.Entry> entries = boundedZipReader.read(zip(files));

        assertThat(entries)
            .extracting(BoundedZipReader.Entry::name)
            .containsExactly("project.json", "workflow-1.json");
        assertThat(new String(entries.getFirst()
            .data(), StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"Test\"}");
    }

    @Test
    void testReadRejectsOversizedEntry() throws IOException {
        BoundedZipReader boundedZipReader = new BoundedZipReader(10, 1024, 1024 * 1024);

        byte[] zip = zip(Map.of("workflow-1.json", new byte[1025]));

        assertThatThrownBy(() -> boundedZipReader.read(zip))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entry exceeds");
    }

    @Test
    void testReadRejectsExcessiveTotalSize() throws IOException {
        BoundedZipReader boundedZipReader = new BoundedZipReader(10, 1024, 2048);

        Map<String, byte[]> files = new LinkedHashMap<>();

        for (int index = 0; index < 3; index++) {
            files.put("workflow-" + index + ".json", new byte[1024]);
        }

        byte[] zip = zip(files);

        assertThatThrownBy(() -> boundedZipReader.read(zip))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("total uncompressed size");
    }

    @Test
    void testReadRejectsTooManyEntries() throws IOException {
        BoundedZipReader boundedZipReader = new BoundedZipReader(2, 1024, 4096);

        Map<String, byte[]> files = new LinkedHashMap<>();

        for (int index = 0; index < 3; index++) {
            files.put("workflow-" + index + ".json", new byte[] {
                'x'
            });
        }

        byte[] zip = zip(files);

        assertThatThrownBy(() -> boundedZipReader.read(zip))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entry count");
    }

    @Test
    void testReadWithoutLimitsReadsEverything() throws IOException {
        BoundedZipReader boundedZipReader = new BoundedZipReader(0, 0, 0);

        Map<String, byte[]> files = new LinkedHashMap<>();

        for (int index = 0; index < 3; index++) {
            files.put("workflow-" + index + ".json", new byte[4096]);
        }

        assertThat(boundedZipReader.read(zip(files))).hasSize(3);
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
