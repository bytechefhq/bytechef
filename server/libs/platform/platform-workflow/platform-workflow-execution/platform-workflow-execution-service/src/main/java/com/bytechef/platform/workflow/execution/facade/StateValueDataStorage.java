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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.StateValueReference;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Component
public class StateValueDataStorage {

    static final String TASK_STATE_COMPONENT_NAME = "__TASK_STATE";
    static final String TRIGGER_STATE_COMPONENT_NAME = "__TRIGGER_STATE";

    private final ApplicationProperties applicationProperties;
    private final ObjectProvider<DataStorage> dataStorageObjectProvider;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobService principalJobService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @SuppressFBWarnings("EI")
    StateValueDataStorage(
        ApplicationProperties applicationProperties, ObjectProvider<DataStorage> dataStorageObjectProvider,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        PlatformTransactionManager platformTransactionManager,
        PrincipalJobService principalJobService) {

        this.applicationProperties = applicationProperties;
        this.dataStorageObjectProvider = dataStorageObjectProvider;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobService = principalJobService;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requiresNewTransactionTemplate = transactionTemplate;
    }

    void delete(Key key, @Nullable Object storedValue, Supplier<Optional<Object>> lockedStoredValueSupplier) {
        if (storedValue instanceof StateValueReference reference) {
            runAfterCompletion(
                () -> {
                    Set<String> storageKeys = new LinkedHashSet<>(
                        List.of(getSlotKey(key, 0), getSlotKey(key, 1), reference.key()));

                    deleteStaleEntries(key, reference, lockedStoredValueSupplier, storageKeys.toArray(String[]::new));
                },
                null);
        }
    }

    Optional<Location> fetchJobLocation(long jobId) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        for (PlatformType type : PlatformType.values()) {
            Optional<Long> jobPrincipalIdOptional = principalJobService.fetchJobPrincipalId(jobId, type);

            if (jobPrincipalIdOptional.isPresent()) {
                return fetchLocation(jobPrincipalIdOptional.get(), type);
            }
        }

        return Optional.empty();
    }

    Optional<Location> fetchLocation(long jobPrincipalId, @Nullable PlatformType type) {
        if (type == null || !isEnabled()) {
            return Optional.empty();
        }

        Optional<JobPrincipalAccessor> jobPrincipalAccessorOptional =
            jobPrincipalAccessorRegistry.fetchJobPrincipalAccessor(type);

        return jobPrincipalAccessorOptional.map(
            jobPrincipalAccessor -> new Location(jobPrincipalAccessor.getEnvironmentId(jobPrincipalId), type));
    }

    @Nullable
    Object read(Key key, @Nullable Object storedValue) {
        if (storedValue instanceof StateValueReference reference) {
            DataStorage dataStorage = getDataStorage();

            Optional<Object> valueOptional = dataStorage.fetch(
                key.componentName(), key.scope(), key.scopeId(), reference.key(), reference.environmentId(),
                reference.type());

            return valueOptional.orElse(null);
        }

        return storedValue;
    }

    Object write(
        Key key, Object value, Optional<Location> locationOptional, @Nullable Object previousStoredValue,
        Supplier<Optional<Object>> lockedStoredValueSupplier) {

        Object storedValue = value;

        if (locationOptional.isPresent()) {
            Location location = locationOptional.get();
            StateValueReference reference = new StateValueReference(
                location.environmentId(), location.type(), getNextStorageKey(key, previousStoredValue));

            DataStorage dataStorage = getDataStorage();

            dataStorage.put(
                key.componentName(), key.scope(), key.scopeId(), reference.key(), value, reference.environmentId(),
                reference.type());

            runAfterCompletion(
                null, () -> deleteStaleEntries(key, reference, lockedStoredValueSupplier, reference.key()));

            storedValue = reference;
        }

        if (previousStoredValue instanceof StateValueReference previousReference) {
            runAfterCompletion(
                () -> deleteStaleEntries(key, previousReference, lockedStoredValueSupplier, previousReference.key()),
                null);
        }

        return storedValue;
    }

    static String toKey(String id) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(messageDigest.digest(id.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private void deleteEntry(Key key, StateValueReference reference, String storageKey) {
        DataStorage dataStorage = getDataStorage();

        dataStorage.delete(
            key.componentName(), key.scope(), key.scopeId(), storageKey, reference.environmentId(),
            reference.type());
    }

    private void deleteStaleEntries(
        Key key, StateValueReference reference, Supplier<Optional<Object>> lockedStoredValueSupplier,
        String... storageKeys) {

        requiresNewTransactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<Object> currentStoredValueOptional = lockedStoredValueSupplier.get();

            String currentKey = currentStoredValueOptional
                .filter(StateValueReference.class::isInstance)
                .map(currentStoredValue -> ((StateValueReference) currentStoredValue).key())
                .orElse(null);

            for (String storageKey : storageKeys) {
                if (!storageKey.equals(currentKey)) {
                    deleteEntry(key, reference, storageKey);
                }
            }
        });
    }

    private static String getNextStorageKey(Key key, @Nullable Object previousStoredValue) {
        if (previousStoredValue == null) {
            return key.key() + "_" + UUID.randomUUID();
        }

        if (previousStoredValue instanceof StateValueReference previousReference) {
            String previousKey = previousReference.key();

            if (previousKey.equals(getSlotKey(key, 0))) {
                return getSlotKey(key, 1);
            }
        }

        return getSlotKey(key, 0);
    }

    private static String getSlotKey(Key key, int slot) {
        return key.key() + "_" + slot;
    }

    private DataStorage getDataStorage() {
        DataStorage dataStorage = dataStorageObjectProvider.getIfAvailable();

        if (dataStorage == null) {
            throw new IllegalStateException("State value is stored in Data Storage, but Data Storage is not available");
        }

        return dataStorage;
    }

    private boolean isEnabled() {
        if (dataStorageObjectProvider.getIfAvailable() == null) {
            return false;
        }

        ApplicationProperties.DataStorage dataStorage = applicationProperties.getDataStorage();

        if (dataStorage == null) {
            return false;
        }

        ApplicationProperties.DataStorage.Provider provider = dataStorage.getProvider();

        return provider != null && provider != ApplicationProperties.DataStorage.Provider.JDBC;
    }

    private static void runAfterCompletion(@Nullable Runnable onCommit, @Nullable Runnable onRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (onCommit != null) {
                onCommit.run();
            }

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED && onCommit != null) {
                    onCommit.run();
                } else if (status == STATUS_ROLLED_BACK && onRollback != null) {
                    onRollback.run();
                }
            }
        });
    }

    record Key(String componentName, DataStorageScope scope, String scopeId, String key) {
    }

    record Location(long environmentId, PlatformType type) {
    }
}
