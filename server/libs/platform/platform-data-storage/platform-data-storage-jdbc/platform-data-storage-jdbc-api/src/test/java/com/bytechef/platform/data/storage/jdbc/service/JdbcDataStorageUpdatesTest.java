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
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntryVersion;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JdbcDataStorageUpdatesTest {

    private static final Function<Optional<Number>, Number> INCREMENT =
        currentValue -> currentValue.map(Number::intValue)
            .orElse(0) + 1;

    @Test
    void testKeepsRetryingPastTwentyConflictsWithinTheTimeBudget() {
        ConflictingJdbcDataStorageService conflictingJdbcDataStorageService = new ConflictingJdbcDataStorageService(30);

        Number result = JdbcDataStorageUpdates.update(
            conflictingJdbcDataStorageService, "component", DataStorageScope.WORKFLOW, "1", "counter", 0,
            PlatformType.AUTOMATION, INCREMENT);

        assertThat(result.intValue()).isEqualTo(1);
        assertThat(conflictingJdbcDataStorageService.attempts.get()).isEqualTo(31);
    }

    @Test
    void testGivesUpWhenTheTimeBudgetRunsOut() {
        ConflictingJdbcDataStorageService conflictingJdbcDataStorageService = new ConflictingJdbcDataStorageService(
            Integer.MAX_VALUE);

        assertThatThrownBy(() -> JdbcDataStorageUpdates.update(
            conflictingJdbcDataStorageService, "component", DataStorageScope.WORKFLOW, "1", "counter", 0,
            PlatformType.AUTOMATION, INCREMENT, Duration.ofMillis(200)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Too many concurrent updates for data storage key 'counter'");
    }

    private static final class ConflictingJdbcDataStorageService implements JdbcDataStorageService {

        private final AtomicInteger attempts = new AtomicInteger();
        private final int conflicts;

        private ConflictingJdbcDataStorageService(int conflicts) {
            this.conflicts = conflicts;
        }

        @Override
        public boolean putIfVersion(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type, Object value, @Nullable DataEntryVersion expectedVersion) {

            return attempts.incrementAndGet() > conflicts;
        }

        @Override
        public <T> Optional<VersionedValue<T>> fetchVersioned(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            return Optional.empty();
        }

        @Override
        public void delete(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Optional<T> fetch(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Map<String, T> getAll(
            String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public void put(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type, Object value) {

            throw new UnsupportedOperationException();
        }
    }
}
