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

package com.bytechef.platform.component.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.cache.SharedCacheSupport;
import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.cache.RedisCacheWriter.RedisCacheWriterConfigurer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Testcontainers
class InMemoryDataStorageIntTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
        .withExposedPorts(6379)
        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    private static final String COMPONENT_NAME = "dataStorage";
    private static final String SCOPE_ID = "1";

    private final String workflowUuid = String.valueOf(UUID.randomUUID());

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testPutValueIsReadByAnotherInstance(CacheManager cacheManager) {
        new InMemoryDataStorage(workflowUuid, cacheManager).put(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "key", Map.of("name", "value"));

        Optional<Map<String, Object>> value = new InMemoryDataStorage(workflowUuid, cacheManager).fetch(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "key");

        assertThat(value).contains(Map.of("name", "value"));
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testAppendingToAFetchedListAndPuttingItBackKeepsEveryItem(CacheManager cacheManager) {
        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        for (int index = 0; index < 3; index++) {
            Optional<List<Object>> currentList = inMemoryDataStorage.fetch(
                COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list");

            List<Object> list = new ArrayList<>(currentList.orElseGet(List::of));

            list.add(index);

            inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list", list);
        }

        assertThat(inMemoryDataStorage.<List<Object>>get(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list"))
            .containsExactly(0, 1, 2);
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testFetchedValueIsNotChangedByLaterWrites(CacheManager cacheManager) {
        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list", List.of("first"));

        List<Object> fetchedList = inMemoryDataStorage.get(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list");

        inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list",
            List.of("first", "second"));

        assertThat(fetchedList).containsExactly("first");
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testValuesKeepTheirTypes(CacheManager cacheManager) {
        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "date", LocalDate.of(2026, 11, 3));
        inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "long", 42L);

        assertThat(inMemoryDataStorage.<Object>get(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "date"))
            .isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(inMemoryDataStorage.<Object>get(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "long"))
            .isEqualTo(42L);
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testDeleteRemovesOnlyTheKey(CacheManager cacheManager) {
        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "first", "1");
        inMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "second", "2");

        inMemoryDataStorage.delete(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "first");

        assertThat(inMemoryDataStorage.<String>getAll(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID))
            .isEqualTo(Map.of("second", "2"));
    }

    private static Stream<Arguments> cacheManagers() {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        CacheConfiguration cacheConfiguration = new CacheConfiguration(
            new StandardEnvironment(), staticListableBeanFactory.getBeanProvider(SharedCacheSupport.class));

        return Stream.of(
            Arguments.of(Named.of("caffeine", cacheConfiguration.cacheManager())),
            Arguments.of(Named.of("redis", createRedisCacheManager())));
    }

    private static RedisCacheManager createRedisCacheManager() {
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));

        lettuceConnectionFactory.afterPropertiesSet();

        try (RedisConnection redisConnection = lettuceConnectionFactory.getConnection()) {
            assertThat(redisConnection.ping()).isEqualTo("PONG");
        }

        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(new JsonRedisSerializer()));

        RedisCacheManager redisCacheManager = RedisCacheManager
            .builder(RedisCacheWriter.create(lettuceConnectionFactory, RedisCacheWriterConfigurer::immediateWrites))
            .cacheDefaults(redisCacheConfiguration)
            .build();

        redisCacheManager.afterPropertiesSet();

        return redisCacheManager;
    }

    private static final class JsonRedisSerializer implements RedisSerializer<Object> {

        @Override
        public byte[] serialize(@Nullable Object value) {
            return value == null ? new byte[0] : JsonUtils.writeValueAsBytes(value);
        }

        @Override
        public @Nullable Object deserialize(byte @Nullable [] bytes) {
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            return JsonUtils.read(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testConcurrentUpdatesFromSeparateInstancesKeepEveryItem(CacheManager cacheManager) throws Exception {
        runConcurrently(8, 25, (threadIndex, iteration) -> {
            InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

            appendValue(inMemoryDataStorage, "list", threadIndex * 25 + iteration);
        });

        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        assertThat(inMemoryDataStorage.<List<Integer>>get(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "list"))
            .hasSize(200);
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testConcurrentPutsOfDifferentKeysInOneScopeKeepEveryKey(CacheManager cacheManager) throws Exception {
        runConcurrently(8, 25, (threadIndex, iteration) -> {
            InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

            inMemoryDataStorage.put(
                COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, "key" + (threadIndex * 25 + iteration),
                iteration);
        });

        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        assertThat(inMemoryDataStorage.getAll(COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID))
            .hasSize(200);
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testEvictingAJobDropsItsCurrentExecutionValuesOfEveryComponent(CacheManager cacheManager) {
        InMemoryDataStorage firstInMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);
        InMemoryDataStorage secondInMemoryDataStorage = new InMemoryDataStorage(
            String.valueOf(UUID.randomUUID()), cacheManager);

        firstInMemoryDataStorage.put("a", DataStorageScope.CURRENT_EXECUTION, "7", "key", "a7");
        secondInMemoryDataStorage.put("b", DataStorageScope.CURRENT_EXECUTION, "7", "key", "b7");
        firstInMemoryDataStorage.put("a", DataStorageScope.CURRENT_EXECUTION, "8", "key", "a8");
        firstInMemoryDataStorage.put("a", DataStorageScope.WORKFLOW, "7", "key", "workflow");

        InMemoryDataStorage.evictJob(cacheManager, 7);

        assertThat(firstInMemoryDataStorage.fetch("a", DataStorageScope.CURRENT_EXECUTION, "7", "key")).isEmpty();
        assertThat(secondInMemoryDataStorage.fetch("b", DataStorageScope.CURRENT_EXECUTION, "7", "key")).isEmpty();
        assertThat(firstInMemoryDataStorage.<String>fetch("a", DataStorageScope.CURRENT_EXECUTION, "8", "key"))
            .contains("a8");
        assertThat(firstInMemoryDataStorage.<String>fetch("a", DataStorageScope.WORKFLOW, "7", "key"))
            .contains("workflow");
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testCurrentExecutionValuesWithoutAJobStayPerWorkflow(CacheManager cacheManager) {
        InMemoryDataStorage firstInMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);
        InMemoryDataStorage secondInMemoryDataStorage = new InMemoryDataStorage(
            String.valueOf(UUID.randomUUID()), cacheManager);

        firstInMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION, "null", "key", "first");
        secondInMemoryDataStorage.put(COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION, "null", "key", "second");

        assertThat(
            firstInMemoryDataStorage.<String>fetch(COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION, "null", "key"))
                .contains("first");
        assertThat(
            secondInMemoryDataStorage.<String>fetch(COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION, "null", "key"))
                .contains("second");
    }

    @ParameterizedTest
    @MethodSource("cacheManagers")
    void testConcurrentUpdatesOfOneJobFromDifferentComponentsKeepEveryValue(CacheManager cacheManager)
        throws Exception {

        runConcurrently(8, 25, (threadIndex, iteration) -> {
            InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

            inMemoryDataStorage.update(
                threadIndex % 2 == 0 ? "a" : "b", DataStorageScope.CURRENT_EXECUTION, "9", "counter",
                (Optional<Number> currentValue) -> (Number) (currentValue.map(Number::intValue)
                    .orElse(0) + 1));
        });

        InMemoryDataStorage inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);

        assertThat(inMemoryDataStorage.<Number>get("a", DataStorageScope.CURRENT_EXECUTION, "9", "counter"))
            .satisfies(counter -> assertThat(counter.intValue()).isEqualTo(100));
        assertThat(inMemoryDataStorage.<Number>get("b", DataStorageScope.CURRENT_EXECUTION, "9", "counter"))
            .satisfies(counter -> assertThat(counter.intValue()).isEqualTo(100));
    }

    private void appendValue(InMemoryDataStorage inMemoryDataStorage, String key, int value) {
        inMemoryDataStorage.update(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, (Optional<List<Integer>> currentValue) -> {
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
