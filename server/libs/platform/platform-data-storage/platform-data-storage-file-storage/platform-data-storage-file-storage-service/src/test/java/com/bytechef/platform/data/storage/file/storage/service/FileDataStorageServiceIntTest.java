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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.file.storage.config.FileDataStorageIntTestConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = FileDataStorageIntTestConfiguration.class, properties = "bytechef.data-storage.provider=filesystem")
public class FileDataStorageServiceIntTest {

    @Autowired
    private DataStorage dataStorage;

    @Test
    public void testDelete() {
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, PlatformType.AUTOMATION);

        String value1 = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals("value1", value1);

        dataStorage.delete("test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0,
            PlatformType.AUTOMATION);

        Optional<String> value1Optional = dataStorage.fetch(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals(Optional.empty(), value1Optional);
    }

    @Test
    public void testFetch() {
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, PlatformType.AUTOMATION);

        Optional<String> value1Optional = dataStorage.fetch(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals("value1", value1Optional.get());
    }

    @Test
    public void testGet() {
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, PlatformType.AUTOMATION);

        String value1 = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals("value1", value1);
    }

    @Test
    public void testGetAll() {
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, PlatformType.AUTOMATION);
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key2", "value2", 0, PlatformType.AUTOMATION);

        Map<String, String> values = dataStorage.getAll(
            "test", DataStorageScope.WORKFLOW, "scopeId1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals(values.size(), 2);
        Assertions.assertEquals("value1", values.get("key1"));
        Assertions.assertEquals("value2", values.get("key2"));
    }

    @Test
    public void testGetAllReturnsOnlyTheEntriesOfTheComponentAndScope() {
        dataStorage.put(
            "listed", DataStorageScope.WORKFLOW, "scopeId2", "first_key", "value1", 1, PlatformType.AUTOMATION);
        dataStorage.put(
            "other", DataStorageScope.WORKFLOW, "scopeId2", "key1", "value2", 1, PlatformType.AUTOMATION);
        dataStorage.put(
            "__internal_state", DataStorageScope.WORKFLOW, "scopeId2", "key1", "value3", 1, PlatformType.AUTOMATION);
        dataStorage.put(
            "listed", DataStorageScope.ACCOUNT, "scopeId2", "key1", "value4", 1, PlatformType.AUTOMATION);

        Map<String, String> values = dataStorage.getAll(
            "listed", DataStorageScope.WORKFLOW, "scopeId2", 1, PlatformType.AUTOMATION);

        Assertions.assertEquals(Map.of("first_key", "value1"), values);
    }

    @Test
    public void testPut() {
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value1", 0, PlatformType.AUTOMATION);

        Object value = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals("value1", value);

        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", "value2", 0, PlatformType.AUTOMATION);

        value = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals("value2", value);

        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 34, 0, PlatformType.AUTOMATION);

        value = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals(34, value);

        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 44L, 0, PlatformType.AUTOMATION);

        value = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals(44L, value);

        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", Map.of("key1", "value1"), 0,
            PlatformType.AUTOMATION);

        value = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals(Map.of("key1", "value1"), value);

        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", Map.of("key1", 23), 0, PlatformType.AUTOMATION);

        value = dataStorage.get(
            "test", DataStorageScope.WORKFLOW, "scopeId1", "key1", 0, PlatformType.AUTOMATION);

        Assertions.assertEquals(Map.of("key1", 23), value);
    }
}
