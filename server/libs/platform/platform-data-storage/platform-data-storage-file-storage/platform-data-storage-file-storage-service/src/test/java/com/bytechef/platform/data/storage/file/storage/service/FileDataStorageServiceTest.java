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

package com.bytechef.platform.data.storage.file.storage.service;

import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class FileDataStorageServiceTest {

    private static final FileDataStorageService FILE_DATA_STORAGE_SERVICE;

    static {
        try {
            Path fileDataStorageTest = Files.createTempDirectory("file_data_storage_test");

            File baseDir = fileDataStorageTest.toFile();

            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new RuntimeException("Failed to create base directory");
            }

            FILE_DATA_STORAGE_SERVICE = new FileDataStorageServiceImpl(
                new FilesystemFileStorageService(baseDir.getAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDelete() {
        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, ModeType.AUTOMATION);

        String value1 = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals("value1", value1);

        FILE_DATA_STORAGE_SERVICE.delete("test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Optional<String> value1Optional = FILE_DATA_STORAGE_SERVICE.fetch(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals(Optional.empty(), value1Optional);
    }

    @Test
    public void testFetch() {
        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, ModeType.AUTOMATION);

        Optional<String> value1Optional = FILE_DATA_STORAGE_SERVICE.fetch(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals("value1", value1Optional.get());
    }

    @Test
    public void testGet() {
        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, ModeType.AUTOMATION);

        String value1 = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals("value1", value1);
    }

    @Test
    public void testGetAll() {
        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, ModeType.AUTOMATION);
        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key2", "value2", 0, ModeType.AUTOMATION);

        Map<String, String> values = FILE_DATA_STORAGE_SERVICE.getAll(
            "test", DataStorageScope.WORKFLOW, "scopeId1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals(values.size(), 2);
        Assertions.assertEquals("value1", values.get("key1"));
        Assertions.assertEquals("value2", values.get("key2"));
    }

    @Test
    public void testPut() {
        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, ModeType.AUTOMATION);

        Object value = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals("value1", value);

        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value2", 0, ModeType.AUTOMATION);

        value = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals("value2", value);

        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 34, 0, ModeType.AUTOMATION);

        value = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals(34, value);

        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 44L, 0, ModeType.AUTOMATION);

        value = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals(44L, value);

        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", Map.of("key1", "value1"), 0, ModeType.AUTOMATION);

        value = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals(Map.of("key1", "value1"), value);

        FILE_DATA_STORAGE_SERVICE.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", Map.of("key1", 23), 0, ModeType.AUTOMATION);

        value = FILE_DATA_STORAGE_SERVICE.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, ModeType.AUTOMATION);

        Assertions.assertEquals(Map.of("key1", 23), value);
    }
}
