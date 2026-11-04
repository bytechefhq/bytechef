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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.file.storage.config.FileDataStorageIntTestConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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

    @Autowired
    private FilesystemFileStorageService filesystemFileStorageService;

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

    @Test
    public void testConcurrentUpdatesKeepEveryItem() throws Exception {
        String scopeId = String.valueOf(UUID.randomUUID());

        runConcurrently(
            8, 25, (threadIndex, iteration) -> appendValue(scopeId, "list", threadIndex * 25 + iteration));

        List<Integer> values = dataStorage.get(
            "componentName", DataStorageScope.WORKFLOW, scopeId, "list", 0, PlatformType.AUTOMATION);

        assertThat(values).hasSize(200);
    }

    @Test
    public void testPutWaitsForARunningUpdateOfTheSameKey() throws Exception {
        String scopeId = String.valueOf(UUID.randomUUID());

        dataStorage.put(
            "componentName", DataStorageScope.WORKFLOW, scopeId, "list", List.of("a"), 0, PlatformType.AUTOMATION);

        CountDownLatch updateStartedLatch = new CountDownLatch(1);
        CountDownLatch putFinishedLatch = new CountDownLatch(1);
        AtomicBoolean putFinishedDuringUpdate = new AtomicBoolean();

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<?> updateFuture = executorService.submit(() -> dataStorage.update(
                "componentName", DataStorageScope.WORKFLOW, scopeId, "list", 0, PlatformType.AUTOMATION,
                (Optional<List<String>> currentValue) -> {
                    updateStartedLatch.countDown();

                    try {
                        putFinishedDuringUpdate.set(putFinishedLatch.await(500, TimeUnit.MILLISECONDS));
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread()
                            .interrupt();
                    }

                    List<String> values = new ArrayList<>(currentValue.orElse(List.of()));

                    values.add("b");

                    return values;
                }));

            assertThat(updateStartedLatch.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> putFuture = executorService.submit(() -> {
                dataStorage.put(
                    "componentName", DataStorageScope.WORKFLOW, scopeId, "list", List.of("x"), 0,
                    PlatformType.AUTOMATION);

                putFinishedLatch.countDown();
            });

            updateFuture.get();
            putFuture.get();
        }

        assertThat(putFinishedDuringUpdate).isFalse();
        assertThat(
            dataStorage.<List<String>>get(
                "componentName", DataStorageScope.WORKFLOW, scopeId, "list", 0, PlatformType.AUTOMATION))
                    .containsExactly("x");
    }

    @Test
    public void testPutStoresTheValueInTheDirectoryOfItsScope() {
        String scopeId = String.valueOf(UUID.randomUUID());

        dataStorage.put("test", DataStorageScope.WORKFLOW, scopeId, "key1", "value1", 2, PlatformType.EMBEDDED);

        assertThat(
            filesystemFileStorageService.fileExists(
                getDirectory(DataStorageScope.WORKFLOW, scopeId, 2, PlatformType.EMBEDDED), "test_key1"))
                    .isTrue();
        assertThat(
            filesystemFileStorageService.fileExists(
                getOldDirectory(2, PlatformType.EMBEDDED),
                getOldFilename("test", DataStorageScope.WORKFLOW, scopeId, "key1")))
                    .isFalse();
    }

    @Test
    public void testDeleteScopeDataDeletesTheValuesOfTheScopeInEveryEnvironment() {
        String scopeId = String.valueOf(UUID.randomUUID());
        String otherScopeId = String.valueOf(UUID.randomUUID());

        dataStorage.put("test", DataStorageScope.WORKFLOW, scopeId, "key1", "value1", 0, PlatformType.AUTOMATION);
        dataStorage.put("other", DataStorageScope.WORKFLOW, scopeId, "key2", "value2", 2, PlatformType.AUTOMATION);
        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, otherScopeId, "key1", "value3", 0, PlatformType.AUTOMATION);
        dataStorage.put("test", DataStorageScope.PRINCIPAL, scopeId, "key1", "value4", 0, PlatformType.AUTOMATION);
        dataStorage.put("test", DataStorageScope.WORKFLOW, scopeId, "key1", "value5", 0, PlatformType.EMBEDDED);

        dataStorage.deleteScopeData(DataStorageScope.WORKFLOW, scopeId, PlatformType.AUTOMATION);

        assertThat(
            dataStorage.fetch("test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(
            dataStorage.fetch("other", DataStorageScope.WORKFLOW, scopeId, "key2", 2, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(
            dataStorage.<String>get(
                "test", DataStorageScope.WORKFLOW, otherScopeId, "key1", 0, PlatformType.AUTOMATION))
                    .isEqualTo("value3");
        assertThat(
            dataStorage.<String>get("test", DataStorageScope.PRINCIPAL, scopeId, "key1", 0, PlatformType.AUTOMATION))
                .isEqualTo("value4");
        assertThat(
            dataStorage.<String>get("test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.EMBEDDED))
                .isEqualTo("value5");
    }

    @Test
    public void testDeleteScopeDataOfAScopeWithoutValuesDoesNothing() {
        dataStorage.deleteScopeData(
            DataStorageScope.CURRENT_EXECUTION, String.valueOf(UUID.randomUUID()), PlatformType.AUTOMATION);
    }

    @Test
    public void testReadsAValueStoredUnderTheOldName() {
        String scopeId = String.valueOf(UUID.randomUUID());

        storeUnderOldName("test", scopeId, "key1", Map.of("count", 3));

        assertThat(
            dataStorage.<Map<String, Integer>>get(
                "test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.AUTOMATION))
                    .isEqualTo(Map.of("count", 3));
        assertThat(
            dataStorage.<Map<String, Integer>>fetch(
                "test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.AUTOMATION))
                    .contains(Map.of("count", 3));
    }

    @Test
    public void testPutMovesAValueStoredUnderTheOldName() {
        String scopeId = String.valueOf(UUID.randomUUID());

        storeUnderOldName("test", scopeId, "key1", "old");

        dataStorage.put("test", DataStorageScope.WORKFLOW, scopeId, "key1", "new", 0, PlatformType.AUTOMATION);

        assertThat(
            filesystemFileStorageService.fileExists(
                getOldDirectory(0, PlatformType.AUTOMATION),
                getOldFilename("test", DataStorageScope.WORKFLOW, scopeId, "key1")))
                    .isFalse();
        assertThat(
            dataStorage.<String>get("test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.AUTOMATION))
                .isEqualTo("new");
    }

    @Test
    public void testUpdateStartsFromAValueStoredUnderTheOldName() {
        String scopeId = String.valueOf(UUID.randomUUID());

        storeUnderOldName("test", scopeId, "list", List.of(1));

        appendValue(scopeId, "test", "list", 2);

        assertThat(
            dataStorage.<List<Integer>>get(
                "test", DataStorageScope.WORKFLOW, scopeId, "list", 0, PlatformType.AUTOMATION))
                    .containsExactly(1, 2);
        assertThat(
            filesystemFileStorageService.fileExists(
                getOldDirectory(0, PlatformType.AUTOMATION),
                getOldFilename("test", DataStorageScope.WORKFLOW, scopeId, "list")))
                    .isFalse();
    }

    @Test
    public void testDeleteRemovesAValueStoredUnderTheOldName() {
        String scopeId = String.valueOf(UUID.randomUUID());

        storeUnderOldName("test", scopeId, "key1", "old");

        dataStorage.delete("test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.AUTOMATION);

        assertThat(
            dataStorage.fetch("test", DataStorageScope.WORKFLOW, scopeId, "key1", 0, PlatformType.AUTOMATION))
                .isEmpty();
    }

    @Test
    public void testGetAllMergesBothNamesAndPrefersTheNewOne() {
        String scopeId = String.valueOf(UUID.randomUUID());

        storeUnderOldName("test", scopeId, "key1", "old1");
        storeUnderOldName("test", scopeId, "key2", "old2");
        storeUnderOldName("test", scopeId + "1", "key3", "otherScope");
        storeUnderOldName("testing", scopeId, "key4", "otherComponent");

        dataStorage.put("test", DataStorageScope.WORKFLOW, scopeId, "key3", "new3", 0, PlatformType.AUTOMATION);
        dataStorage.put("other", DataStorageScope.WORKFLOW, scopeId, "key5", "new5", 0, PlatformType.AUTOMATION);

        storeUnderOldName("test", scopeId, "key3", "stale3");

        Map<String, String> values = dataStorage.getAll(
            "test", DataStorageScope.WORKFLOW, scopeId, 0, PlatformType.AUTOMATION);

        assertThat(values).isEqualTo(Map.of("key1", "old1", "key2", "old2", "key3", "new3"));
    }

    private void storeUnderOldName(String componentName, String scopeId, String key, Object value) {
        String directory = getDirectory(DataStorageScope.WORKFLOW, scopeId, 0, PlatformType.AUTOMATION);
        String temporaryKey = key + "_" + UUID.randomUUID();

        dataStorage.put(
            componentName, DataStorageScope.WORKFLOW, scopeId, temporaryKey, value, 0, PlatformType.AUTOMATION);

        FileEntry fileEntry = filesystemFileStorageService.getFileEntry(
            directory, componentName + "_" + temporaryKey);

        byte[] content = filesystemFileStorageService.readFileToBytes(directory, fileEntry);

        filesystemFileStorageService.deleteFile(directory, fileEntry);

        filesystemFileStorageService.storeFileContent(
            getOldDirectory(0, PlatformType.AUTOMATION),
            getOldFilename(componentName, DataStorageScope.WORKFLOW, scopeId, key), content, false);
    }

    private static String getDirectory(
        DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

        return "data_storage/" + type.ordinal() + "/" + scope.ordinal() + "/" + scopeId + "/" + environmentId;
    }

    private static String getOldDirectory(long environmentId, PlatformType type) {
        return "data_entries/" + type.ordinal() + "/" + environmentId;
    }

    private static String getOldFilename(
        String componentName, DataStorageScope scope, String scopeId, String key) {

        return componentName + "_" + scope.ordinal() + "_" + scopeId + "_" + key;
    }

    private void appendValue(String scopeId, String componentName, String key, int value) {
        dataStorage.update(
            componentName, DataStorageScope.WORKFLOW, scopeId, key, 0, PlatformType.AUTOMATION,
            (Optional<List<Integer>> currentValue) -> {
                List<Integer> values = new ArrayList<>(currentValue.orElse(List.of()));

                values.add(value);

                return values;
            });
    }

    private void appendValue(String scopeId, String key, int value) {
        dataStorage.update(
            "componentName", DataStorageScope.WORKFLOW, scopeId, key, 0, PlatformType.AUTOMATION,
            (Optional<List<Integer>> currentValue) -> {
                List<Integer> values = new ArrayList<>(currentValue.orElse(List.of()));

                values.add(value);

                return values;
            });
    }

    private static void runConcurrently(int threadCount, int iterations, BiConsumer<Integer, Integer> work)
        throws Exception {

        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
                int currentThreadIndex = threadIndex;

                futures.add(executorService.submit(() -> {
                    startLatch.await();

                    for (int iteration = 0; iteration < iterations; iteration++) {
                        work.accept(currentThreadIndex, iteration);
                    }

                    return null;
                }));
            }

            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get();
            }
        }
    }
}
