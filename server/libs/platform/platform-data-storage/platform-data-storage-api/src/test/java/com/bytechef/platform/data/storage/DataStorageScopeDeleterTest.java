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

package com.bytechef.platform.data.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
class DataStorageScopeDeleterTest {

    private static final List<ScopeData> SCOPE_DATA_LIST = List.of(
        new ScopeData(DataStorageScope.PRINCIPAL, "1", PlatformType.AUTOMATION),
        new ScopeData(DataStorageScope.WORKFLOW, "2", PlatformType.AUTOMATION),
        new ScopeData(DataStorageScope.WORKFLOW, "3", PlatformType.EMBEDDED));

    private final RecordingDataStorage recordingDataStorage = new RecordingDataStorage();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    @AfterEach
    void afterEach() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void testDeletesRightAwayWithoutATransaction() {
        DataStorageScopeDeleter.deleteAfterCommit(recordingDataStorage, transactionManager, SCOPE_DATA_LIST);

        assertThat(recordingDataStorage.deletedScopeDataList).containsExactlyElementsOf(SCOPE_DATA_LIST);
    }

    @Test
    void testDeletesRightAwayInANonTransactionalScopeWithSynchronization() {
        TransactionSynchronizationManager.initSynchronization();

        DataStorageScopeDeleter.deleteAfterCommit(recordingDataStorage, transactionManager, SCOPE_DATA_LIST);

        assertThat(recordingDataStorage.deletedScopeDataList).containsExactlyElementsOf(SCOPE_DATA_LIST);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void testDeletesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        DataStorageScopeDeleter.deleteAfterCommit(recordingDataStorage, transactionManager, SCOPE_DATA_LIST);

        assertThat(recordingDataStorage.deletedScopeDataList).isEmpty();

        for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
            .getSynchronizations()) {

            transactionSynchronization.afterCommit();
        }

        assertThat(recordingDataStorage.deletedScopeDataList).containsExactlyElementsOf(SCOPE_DATA_LIST);
        assertThat(recordingDataStorage.deletingThreads).containsOnly(Thread.currentThread());
        assertThat(transactionManager.definitions)
            .hasSize(SCOPE_DATA_LIST.size())
            .allSatisfy(definition -> assertThat(definition.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void testDeletesNothingOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        DataStorageScopeDeleter.deleteAfterCommit(recordingDataStorage, transactionManager, SCOPE_DATA_LIST);

        for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
            .getSynchronizations()) {

            transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        assertThat(recordingDataStorage.deletedScopeDataList).isEmpty();
    }

    @Test
    void testContinuesAfterAFailedScope() {
        recordingDataStorage.failingScopeIds = Set.of("2");

        DataStorageScopeDeleter.deleteAfterCommit(recordingDataStorage, transactionManager, SCOPE_DATA_LIST);

        assertThat(recordingDataStorage.deletedScopeDataList).containsExactly(
            SCOPE_DATA_LIST.get(0), SCOPE_DATA_LIST.get(2));
    }

    @Test
    void testIgnoresUnsupportedProvider() {
        recordingDataStorage.unsupported = true;

        DataStorageScopeDeleter.deleteAfterCommit(recordingDataStorage, transactionManager, SCOPE_DATA_LIST);

        assertThat(recordingDataStorage.deletedScopeDataList).isEmpty();
        assertThat(recordingDataStorage.attempts).isEqualTo(SCOPE_DATA_LIST.size());
    }

    @Test
    void testDoesNothingWithoutDataStorage() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        DataStorageScopeDeleter.deleteAfterCommit(null, transactionManager, SCOPE_DATA_LIST);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {

        private final List<TransactionDefinition> definitions = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
            definitions.add(definition);

            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            assertThat(status.isCompleted()).isFalse();
        }

        @Override
        public void rollback(TransactionStatus status) {
            assertThat(status.isCompleted()).isFalse();
        }
    }

    private static final class RecordingDataStorage implements DataStorage {

        private final List<ScopeData> deletedScopeDataList = new ArrayList<>();
        private final List<Thread> deletingThreads = new ArrayList<>();
        private int attempts;
        private Set<String> failingScopeIds = Set.of();
        private boolean unsupported;

        @Override
        public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
            attempts++;

            if (unsupported) {
                throw new UnsupportedOperationException();
            }

            if (failingScopeIds.contains(scopeId)) {
                throw new IllegalStateException("Data Storage is unavailable");
            }

            deletedScopeDataList.add(new ScopeData(scope, scopeId, type));
            deletingThreads.add(Thread.currentThread());
        }

        @Override
        public void delete(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

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
            String componentName, DataStorageScope scope, String scopeId, String key, Object value,
            long environmentId, PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T update(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type, Function<Optional<T>, T> updater) {

            throw new UnsupportedOperationException();
        }
    }
}
