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

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.TriggerContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
class TriggerContextImpl extends ContextImpl implements TriggerContext, TriggerContextAware {

    private final Data data;
    @Nullable
    private final Long jobPrincipalId;
    private final String triggerName;
    @Nullable
    private final PlatformType type;
    @Nullable
    private final String workflowUuid;

    @SuppressFBWarnings("EI")
    public TriggerContextImpl(
        String componentName, int componentVersion, String triggerName, @Nullable Long jobPrincipalId,
        @Nullable String workflowUuid, @Nullable ComponentConnection componentConnection, CacheManager cacheManager,
        DataStorage dataStorage, TempFileStorage tempFileStorage, HttpClientExecutor httpClientExecutor,
        @Nullable Long environmentId, @Nullable PlatformType type, boolean editorEnvironment) {

        super(
            componentName, componentVersion, triggerName, componentConnection, editorEnvironment, httpClientExecutor,
            tempFileStorage);

        this.data = new DataImpl(
            dataStorage, componentName, componentVersion, triggerName, workflowUuid, cacheManager, environmentId, type,
            editorEnvironment);
        this.jobPrincipalId = jobPrincipalId;
        this.triggerName = triggerName;
        this.type = type;
        this.workflowUuid = workflowUuid;
    }

    @Override
    public <R> R data(ContextFunction<Data, R> dataFunction) {
        try {
            return dataFunction.apply(data);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @Nullable
    public Long getJobPrincipalId() {
        return jobPrincipalId;
    }

    @Override
    public String getTriggerName() {
        return triggerName;
    }

    @Override
    @Nullable
    public PlatformType getType() {
        return type;
    }

    @Override
    @Nullable
    public String getWorkflowUuid() {
        return workflowUuid;
    }

    private static final class DataImpl implements Data {

        private final DataStorage dataStorage;
        private final String componentName;
        private final Integer componentVersion;
        private final boolean editorEnvironment;
        @Nullable
        private final Long environmentId;
        private final InMemoryDataStorage inMemoryDataStorage;
        private final String triggerName;
        @Nullable
        private final PlatformType type;
        @Nullable
        private final String workflowUuid;

        private DataImpl(
            DataStorage dataStorage, String componentName, Integer componentVersion, String triggerName,
            @Nullable String workflowUuid, CacheManager cacheManager, @Nullable Long environmentId,
            @Nullable PlatformType type, boolean editorEnvironment) {

            this.componentName = componentName;
            this.componentVersion = componentVersion;
            this.dataStorage = dataStorage;
            this.editorEnvironment = editorEnvironment;
            this.environmentId = environmentId;
            this.inMemoryDataStorage = new InMemoryDataStorage(workflowUuid, cacheManager);
            this.triggerName = triggerName;
            this.type = type;
            this.workflowUuid = workflowUuid;
        }

        @Override
        public <T> Optional<T> fetch(Scope scope, String key) {
            if (editorEnvironment) {
                return inMemoryDataStorage.fetch(componentName, getDataStorageScope(scope), getScopeId(scope), key);
            }

            return dataStorage.fetch(
                componentName, getDataStorageScope(scope), getScopeId(scope), key,
                Objects.requireNonNull(environmentId), Objects.requireNonNull(type));
        }

        @Override
        public <T> T get(Scope scope, String key) {
            if (editorEnvironment) {
                return inMemoryDataStorage.get(componentName, getDataStorageScope(scope), getScopeId(scope), key);
            }

            return dataStorage.get(
                componentName, getDataStorageScope(scope), getScopeId(scope), key,
                Objects.requireNonNull(environmentId), Objects.requireNonNull(type));
        }

        @Override
        public Void put(Scope scope, String key, Object value) {
            if (editorEnvironment) {
                inMemoryDataStorage.put(componentName, getDataStorageScope(scope), getScopeId(scope), key, value);
            } else {
                dataStorage.put(
                    componentName, getDataStorageScope(scope), getScopeId(scope), key, value,
                    Objects.requireNonNull(environmentId), Objects.requireNonNull(type));
            }

            return null;
        }

        @Override
        public Void remove(Scope scope, String key) {
            if (editorEnvironment) {
                inMemoryDataStorage.delete(componentName, getDataStorageScope(scope), getScopeId(scope), key);
            } else {
                dataStorage.delete(
                    componentName, getDataStorageScope(scope), getScopeId(scope), key,
                    Objects.requireNonNull(environmentId), Objects.requireNonNull(type));
            }

            return null;
        }

        private DataStorageScope getDataStorageScope(Scope scope) {
            return switch (scope) {
                case WORKFLOW -> DataStorageScope.WORKFLOW;
                case ACCOUNT -> DataStorageScope.ACCOUNT;
            };
        }

        private String getScopeId(Scope scope) {
            return Validate.notNull(
                switch (scope) {
                    case WORKFLOW -> workflowUuid;
                    case ACCOUNT -> "";
                }, "scope");
        }

        public DataStorage dataStorage() {
            return dataStorage;
        }

        public String componentName() {
            return componentName;
        }

        public Integer componentVersion() {
            return componentVersion;
        }

        public String triggerName() {
            return triggerName;
        }

        @Nullable
        public String workflowUuid() {
            return workflowUuid;
        }

        @Nullable
        public Long environmentId() {
            return environmentId;
        }

        @Nullable
        public PlatformType type() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;
            var that = (DataImpl) obj;
            return Objects.equals(this.dataStorage, that.dataStorage) &&
                Objects.equals(this.componentName, that.componentName) &&
                Objects.equals(this.componentVersion, that.componentVersion) &&
                Objects.equals(this.triggerName, that.triggerName) &&
                Objects.equals(this.workflowUuid, that.workflowUuid) &&
                Objects.equals(this.environmentId, that.environmentId) &&
                Objects.equals(this.type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataStorage, componentName, componentVersion, triggerName, workflowUuid, environmentId,
                type);
        }

        @Override
        public String toString() {
            return "DataImpl[" +
                "dataStorage=" + dataStorage + ", " +
                "componentName=" + componentName + ", " +
                "componentVersion=" + componentVersion + ", " +
                "triggerName=" + triggerName + ", " +
                "workflowUuid=" + workflowUuid + ", " +
                "environmentId=" + environmentId + ", " +
                "type=" + type + ']';
        }

    }
}
