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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.StateValueReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class StateValueDataStorage {

    static final String TASK_STATE_COMPONENT_NAME = "__task_state";
    static final String TRIGGER_STATE_COMPONENT_NAME = "__trigger_state";

    private final ObjectProvider<ApplicationProperties> applicationPropertiesObjectProvider;
    private final ObjectProvider<DataStorage> dataStorageObjectProvider;
    private final ObjectProvider<JobPrincipalAccessorRegistry> jobPrincipalAccessorRegistryObjectProvider;
    private final PrincipalJobService principalJobService;

    @SuppressFBWarnings("EI")
    StateValueDataStorage(
        ObjectProvider<ApplicationProperties> applicationPropertiesObjectProvider,
        ObjectProvider<DataStorage> dataStorageObjectProvider,
        ObjectProvider<JobPrincipalAccessorRegistry> jobPrincipalAccessorRegistryObjectProvider,
        PrincipalJobService principalJobService) {

        this.applicationPropertiesObjectProvider = applicationPropertiesObjectProvider;
        this.dataStorageObjectProvider = dataStorageObjectProvider;
        this.jobPrincipalAccessorRegistryObjectProvider = jobPrincipalAccessorRegistryObjectProvider;
        this.principalJobService = principalJobService;
    }

    void delete(Key key, @Nullable Object storedValue) {
        if (storedValue instanceof StateValueReference reference) {
            DataStorage dataStorage = getDataStorage();

            dataStorage.delete(
                key.componentName(), key.scope(), key.scopeId(), key.key(), reference.environmentId(),
                reference.type());
        }
    }

    Optional<StateValueReference> fetchJobReference(long jobId) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        for (PlatformType type : PlatformType.values()) {
            Optional<Long> jobPrincipalIdOptional = principalJobService.fetchJobPrincipalId(jobId, type);

            if (jobPrincipalIdOptional.isPresent()) {
                return fetchReference(jobPrincipalIdOptional.get(), type);
            }
        }

        return Optional.empty();
    }

    Optional<StateValueReference> fetchReference(long jobPrincipalId, @Nullable PlatformType type) {
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry =
            jobPrincipalAccessorRegistryObjectProvider.getIfAvailable();

        if (type == null || jobPrincipalAccessorRegistry == null || !isEnabled()) {
            return Optional.empty();
        }

        Optional<JobPrincipalAccessor> jobPrincipalAccessorOptional =
            jobPrincipalAccessorRegistry.fetchJobPrincipalAccessor(type);

        return jobPrincipalAccessorOptional.map(
            jobPrincipalAccessor -> new StateValueReference(
                jobPrincipalAccessor.getEnvironmentId(jobPrincipalId), type));
    }

    @Nullable
    Object read(Key key, @Nullable Object storedValue) {
        if (storedValue instanceof StateValueReference reference) {
            DataStorage dataStorage = getDataStorage();

            Optional<Object> valueOptional = dataStorage.fetch(
                key.componentName(), key.scope(), key.scopeId(), key.key(), reference.environmentId(),
                reference.type());

            return valueOptional.orElse(null);
        }

        return storedValue;
    }

    Object write(Key key, Object value, Optional<StateValueReference> referenceOptional) {
        if (referenceOptional.isEmpty()) {
            return value;
        }

        StateValueReference reference = referenceOptional.get();
        DataStorage dataStorage = getDataStorage();

        dataStorage.put(
            key.componentName(), key.scope(), key.scopeId(), key.key(), value, reference.environmentId(),
            reference.type());

        return reference;
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

    private DataStorage getDataStorage() {
        DataStorage dataStorage = dataStorageObjectProvider.getIfAvailable();

        if (dataStorage == null) {
            throw new IllegalStateException("State value is stored in Data Storage, but Data Storage is not available");
        }

        return dataStorage;
    }

    private boolean isEnabled() {
        ApplicationProperties applicationProperties = applicationPropertiesObjectProvider.getIfAvailable();

        if (applicationProperties == null || dataStorageObjectProvider.getIfAvailable() == null) {
            return false;
        }

        ApplicationProperties.DataStorage dataStorage = applicationProperties.getDataStorage();

        if (dataStorage == null) {
            return false;
        }

        ApplicationProperties.DataStorage.Provider provider = dataStorage.getProvider();

        return provider != null && provider != ApplicationProperties.DataStorage.Provider.JDBC;
    }

    record Key(String componentName, DataStorageScope scope, String scopeId, String key) {
    }
}
