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

package com.bytechef.platform.data.storage.file.storage.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.file.storage.domain.VersionedFileContent;
import com.bytechef.file.storage.service.ConditionalFileStorageService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Asish Kumar
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class FileDataStorageFacadeConditionalUpdateTest {

    private static final String COMPONENT_NAME = "test";
    private static final String KEY = "counter";
    private static final String SCOPE_ID = "scopeId";

    @Test
    void testConcurrentUpdatesFromSeveralNodesDoNotLoseChanges() throws Exception {
        ConditionalFileStorageService fileStorageService = createFileStorageService(new InMemoryObjects(false));

        List<FileDataStorageFacadeImpl> nodes = List.of(
            createFacade(fileStorageService), createFacade(fileStorageService), createFacade(fileStorageService));

        int updatesPerNode = 25;

        ExecutorService executorService = Executors.newFixedThreadPool(nodes.size() * 2);

        try {
            List<Future<?>> futures = new ArrayList<>();

            for (FileDataStorageFacadeImpl node : nodes) {
                for (int thread = 0; thread < 2; thread++) {
                    futures.add(executorService.submit(() -> {
                        for (int update = 0; update < updatesPerNode; update++) {
                            increment(node);
                        }
                    }));
                }
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdownNow();
        }

        Optional<Integer> counterOptional = nodes.getFirst()
            .fetch(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, KEY, 0, PlatformType.AUTOMATION);

        assertThat(counterOptional).hasValue(nodes.size() * 2 * updatesPerNode);
    }

    @Test
    void testUpdateCreatesTheValueWhenItDoesNotExist() {
        FileDataStorageFacadeImpl facade = createFacade(createFileStorageService(new InMemoryObjects(false)));

        Integer counter = increment(facade);

        assertThat(counter).isEqualTo(1);
        assertThat(increment(facade)).isEqualTo(2);
    }

    @Test
    void testUpdateFailsWhenTheValueKeepsChanging() {
        ConditionalFileStorageService fileStorageService = createFileStorageService(new InMemoryObjects(true));
        FileDataStorageFacadeImpl facade = createFacade(fileStorageService);

        assertThatThrownBy(() -> facade.updateConditionally(
            fileStorageService, COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, KEY, 0, PlatformType.AUTOMATION,
            optional -> 1, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(KEY);
    }

    @Test
    void testUpdateRejectsANullResult() {
        FileDataStorageFacadeImpl facade = createFacade(createFileStorageService(new InMemoryObjects(false)));

        assertThatThrownBy(() -> facade.update(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, KEY, 0, PlatformType.AUTOMATION, optional -> null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static FileDataStorageFacadeImpl createFacade(ConditionalFileStorageService fileStorageService) {
        return new FileDataStorageFacadeImpl(
            fileStorageService, JsonMapper.builder()
                .build());
    }

    private static Integer increment(FileDataStorageFacadeImpl facade) {
        return facade.update(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, KEY, 0, PlatformType.AUTOMATION,
            (Optional<Integer> counterOptional) -> counterOptional.orElse(0) + 1);
    }

    private static ConditionalFileStorageService createFileStorageService(InMemoryObjects inMemoryObjects) {
        return (ConditionalFileStorageService) Proxy.newProxyInstance(
            FileDataStorageFacadeConditionalUpdateTest.class.getClassLoader(),
            new Class<?>[] {
                ConditionalFileStorageService.class
            },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "fileExists" -> false;
                case "readFileContentVersioned" -> inMemoryObjects.read((String) arguments[0], (String) arguments[1]);
                case "storeFileContentIfVersion" -> inMemoryObjects.storeIfVersion(
                    (String) arguments[0], (String) arguments[1], (byte[]) arguments[2], (String) arguments[3]);
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private static final class InMemoryObjects {

        private final boolean alwaysConflict;
        private final Map<String, VersionedFileContent> objects = new ConcurrentHashMap<>();
        private final AtomicLong versions = new AtomicLong();

        private InMemoryObjects(boolean alwaysConflict) {
            this.alwaysConflict = alwaysConflict;
        }

        private Optional<VersionedFileContent> read(String directory, String filename) {
            return Optional.ofNullable(objects.get(directory + "/" + filename));
        }

        private synchronized boolean storeIfVersion(
            String directory, String filename, byte[] data, String expectedVersion) {

            String path = directory + "/" + filename;
            VersionedFileContent current = objects.get(path);
            String currentVersion = current == null ? null : current.version();

            if (alwaysConflict || !Objects.equals(currentVersion, expectedVersion)) {
                return false;
            }

            objects.put(path, new VersionedFileContent(data, String.valueOf(versions.incrementAndGet())));

            return true;
        }
    }
}
