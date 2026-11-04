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

package com.bytechef.platform.data.storage.jdbc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.config.DataStorageIntTestConfiguration;
import com.bytechef.platform.data.storage.jdbc.config.JdbcDataStorageConfiguration;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntryVersion;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Import({
    JdbcDataStorageConfiguration.class, PostgreSQLContainerConfiguration.class
})
@SpringBootTest(classes = DataStorageIntTestConfiguration.class, properties = "bytechef.data-storage.provider=jdbc")
class JdbcDataStorageServiceIntTest {

    private static final String COMPONENT_NAME = "dataStorage";
    private static final long ENVIRONMENT_ID = 0;
    private static final String SCOPE_ID = "1";

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private DataStorageRepository dataStorageRepository;

    @Autowired
    private JdbcDataStorageService jdbcDataStorageService;

    @AfterEach
    void afterEach() {
        dataStorageRepository.deleteAll();
    }

    @Test
    void testDeleteScopeDataDeletesOnlyTheGivenType() {
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "key", "automation", ENVIRONMENT_ID,
            PlatformType.AUTOMATION);
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "embeddedKey", "embedded", ENVIRONMENT_ID,
            PlatformType.EMBEDDED);
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.PRINCIPAL, "6", "key", "other", ENVIRONMENT_ID, PlatformType.AUTOMATION);

        dataStorage.deleteScopeData(DataStorageScope.PRINCIPAL, "5", PlatformType.AUTOMATION);

        assertThat(fetchPrincipalValue("5", ENVIRONMENT_ID, PlatformType.AUTOMATION)).isEmpty();
        assertThat(
            dataStorage.<String>fetch(
                COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "embeddedKey", ENVIRONMENT_ID, PlatformType.EMBEDDED))
                    .contains("embedded");
        assertThat(fetchPrincipalValue("6", ENVIRONMENT_ID, PlatformType.AUTOMATION)).contains("other");
    }

    @Test
    void testPutKeepsTheSameKeyOfTwoComponentsApart() {
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "count", 1, ENVIRONMENT_ID, PlatformType.AUTOMATION);
        dataStorage.put(
            "otherComponent", DataStorageScope.WORKFLOW, SCOPE_ID, "count", 2, ENVIRONMENT_ID,
            PlatformType.AUTOMATION);

        assertThat(
            dataStorage.<Integer>fetch(
                COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "count", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                    .contains(1);
        assertThat(
            dataStorage.<Integer>fetch(
                "otherComponent", DataStorageScope.WORKFLOW, SCOPE_ID, "count", ENVIRONMENT_ID,
                PlatformType.AUTOMATION))
                    .contains(2);
    }

    @Test
    void testPutKeepsTheSameKeyOfTwoPlatformTypesApart() {
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "key", "automation", ENVIRONMENT_ID,
            PlatformType.AUTOMATION);
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "key", "embedded", ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        assertThat(fetchPrincipalValue("5", ENVIRONMENT_ID, PlatformType.AUTOMATION)).contains("automation");
        assertThat(fetchPrincipalValue("5", ENVIRONMENT_ID, PlatformType.EMBEDDED)).contains("embedded");
    }

    @Test
    void testDeleteScopeDataDeletesEveryEnvironment() {
        dataStorage.put(COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "key", "first", 1, PlatformType.AUTOMATION);
        dataStorage.put(COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "key", "second", 2, PlatformType.AUTOMATION);

        dataStorage.deleteScopeData(DataStorageScope.PRINCIPAL, "5", PlatformType.AUTOMATION);

        assertThat(fetchPrincipalValue("5", 1, PlatformType.AUTOMATION)).isEmpty();
        assertThat(fetchPrincipalValue("5", 2, PlatformType.AUTOMATION)).isEmpty();
    }

    @Test
    void testUpdateKeepsEveryConcurrentAppend() throws Exception {
        putValue("list", List.of());

        runConcurrently(8, 25, (threadIndex, iteration) -> appendValue("list", threadIndex * 25 + iteration));

        List<Integer> values = fetchValue("list");

        assertThat(values).hasSize(200);
        assertThat(values).containsExactlyInAnyOrderElementsOf(
            IntStream.range(0, 200)
                .boxed()
                .toList());
    }

    @Test
    void testUpdateKeepsEveryConcurrentIncrement() throws Exception {
        putValue("counter", 0);

        runConcurrently(8, 25, (threadIndex, iteration) -> incrementValue("counter"));

        Number counter = fetchValue("counter");

        assertThat(counter.intValue()).isEqualTo(200);
    }

    @Test
    void testUpdateCreatesTheEntryOnceWhenThreadsRaceOnTheFirstInsert() throws Exception {
        runConcurrently(8, 1, (threadIndex, iteration) -> appendValue("first", threadIndex));

        List<Integer> values = fetchValue("first");

        assertThat(values).hasSize(8);
        assertThat(dataStorageRepository.count()).isEqualTo(1);
    }

    @Test
    void testPutIfVersionRejectsAStaleVersion() {
        putValue("key", "first");

        DataEntryVersion version = fetchVersion("key");

        assertThat(putIfVersion("key", "second", version)).isTrue();
        assertThat(putIfVersion("key", "third", version)).isFalse();
        assertThat(this.<String>fetchValue("key")).isEqualTo("second");
    }

    @Test
    void testPutIfVersionRejectsARecreatedEntry() {
        putValue("key", "first");

        DataEntryVersion version = fetchVersion("key");

        jdbcDataStorageService.delete(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "key", ENVIRONMENT_ID, PlatformType.AUTOMATION);

        putValue("key", "second");

        assertThat(putIfVersion("key", "third", version)).isFalse();
        assertThat(this.<String>fetchValue("key")).isEqualTo("second");
    }

    @Test
    void testPutIfVersionRejectsCreatingAnExistingEntry() {
        putValue("key", "first");

        assertThat(putIfVersion("key", "second", null)).isFalse();
        assertThat(this.<String>fetchValue("key")).isEqualTo("first");
    }

    @Test
    void testPutIfVersionCreatesAMissingEntry() {
        assertThat(putIfVersion("key", "first", null)).isTrue();
        assertThat(this.<String>fetchValue("key")).isEqualTo("first");
    }

    @Test
    void testUpdateRejectsANullResult() {
        assertThatThrownBy(() -> updateValue("key", (Optional<String> currentValue) -> null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A data storage update must not return null");
    }

    @Test
    void testDataStorageUpdateRejectsAValueOverTheSizeLimit() {
        String largeValue = "x".repeat(409601);

        assertThatThrownBy(() -> dataStorage.update(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "key", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            (Optional<String> currentValue) -> largeValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Value size exceeds 400KB limit per key");
        assertThat(dataStorageRepository.count()).isZero();
    }

    private boolean putIfVersion(String key, Object value, @Nullable DataEntryVersion expectedVersion) {
        return jdbcDataStorageService.putIfVersion(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID, PlatformType.AUTOMATION, value,
            expectedVersion);
    }

    private DataEntryVersion fetchVersion(String key) {
        Optional<VersionedValue<Object>> versionedValue = jdbcDataStorageService.fetchVersioned(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        return versionedValue.orElseThrow()
            .version();
    }

    private void appendValue(String key, int value) {
        updateValue(key, (Optional<List<Integer>> currentValue) -> {
            List<Integer> values = new ArrayList<>(currentValue.orElse(List.of()));

            values.add(value);

            return values;
        });
    }

    private void incrementValue(String key) {
        updateValue(key, (Optional<Number> currentValue) -> (Number) (currentValue.map(Number::intValue)
            .orElse(0) + 1));
    }

    private <T> void updateValue(String key, Function<Optional<T>, T> updater) {
        JdbcDataStorageUpdates.update(
            jdbcDataStorageService, COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID,
            PlatformType.AUTOMATION, updater);
    }

    private void putValue(String key, Object value) {
        jdbcDataStorageService.put(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID, PlatformType.AUTOMATION, value);
    }

    private Optional<String> fetchPrincipalValue(String scopeId, long environmentId, PlatformType type) {
        return dataStorage.fetch(COMPONENT_NAME, DataStorageScope.PRINCIPAL, scopeId, "key", environmentId, type);
    }

    private <T> T fetchValue(String key) {
        Optional<T> value = jdbcDataStorageService.fetch(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        return value.orElseThrow();
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
