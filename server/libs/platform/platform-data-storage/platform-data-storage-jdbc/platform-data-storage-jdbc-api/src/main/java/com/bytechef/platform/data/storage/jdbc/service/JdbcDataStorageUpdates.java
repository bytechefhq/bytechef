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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
public final class JdbcDataStorageUpdates {

    private static final int MAX_ATTEMPTS = 20;

    private JdbcDataStorageUpdates() {
    }

    public static <T> T update(
        JdbcDataStorageService jdbcDataStorageService, String componentName, DataStorageScope scope, String scopeId,
        String key, long environmentId, PlatformType type, Function<Optional<T>, T> updater) {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Optional<VersionedValue<T>> versionedValueOptional = jdbcDataStorageService.fetchVersioned(
                componentName, scope, scopeId, key, environmentId, type);

            T result = updater.apply(versionedValueOptional.map(VersionedValue::value));

            if (result == null) {
                throw new IllegalArgumentException("A data storage update must not return null");
            }

            Integer expectedVersion = versionedValueOptional.map(VersionedValue::version)
                .orElse(null);

            if (jdbcDataStorageService.putIfVersion(
                componentName, scope, scopeId, key, environmentId, type, result, expectedVersion)) {

                return result;
            }

            backOff(attempt);
        }

        throw new IllegalStateException("Too many concurrent updates for data storage key '%s'".formatted(key));
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Retry wait time, not security sensitive")
    private static void backOff(int attempt) {
        long delay = ThreadLocalRandom.current()
            .nextLong(1, Math.min(100, 5L * attempt) + 1);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Interrupted while updating a data storage value", interruptedException);
        }
    }
}
