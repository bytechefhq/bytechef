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
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
class TriggerContextImpl extends ContextImpl implements TriggerContext, TriggerContextAware {

    private final Data data;
    private final boolean editorEnvironment;
    private final Long jobPrincipalId;
    private final String triggerName;
    private final ModeType type;
    private final String workflowUuid;

    @SuppressFBWarnings("EI")
    public TriggerContextImpl(
        String componentName, int componentVersion, @Nullable ComponentConnection connection, DataStorage dataStorage,
        boolean editorEnvironment, TempFileStorage tempFileStorage, HttpClientExecutor httpClientExecutor,
        @Nullable Long jobPrincipalId, String triggerName, @Nullable ModeType type, @Nullable String workflowUuid) {

        super(componentName, componentVersion, triggerName, connection, httpClientExecutor, tempFileStorage);

        this.data = new DataImpl(componentName, componentVersion, triggerName, type, workflowUuid, dataStorage);
        this.editorEnvironment = editorEnvironment;
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
    public Long getJobPrincipalId() {
        return jobPrincipalId;
    }

    @Override
    public String getTriggerName() {
        return triggerName;
    }

    @Override
    public ModeType getType() {
        return type;
    }

    @Override
    public String getWorkflowUuid() {
        return workflowUuid;
    }

    @Override
    public boolean isEditorEnvironment() {
        return editorEnvironment;
    }

    private record DataImpl(
        String componentName, Integer componentVersion, String triggerName, ModeType type,
        String workflowUuid, DataStorage dataStorage) implements Data {

        @Override
        public <T> Optional<T> fetch(Data.Scope scope, String key) {
            return dataStorage.fetch(
                componentName, getDataStorageScope(scope), getScopeId(scope), key, type);
        }

        @Override
        public <T> T get(Data.Scope scope, String key) {
            return dataStorage.get(
                componentName, getDataStorageScope(scope), getScopeId(scope), key, type);
        }

        @Override
        public Void put(Data.Scope scope, String key, Object value) {
            dataStorage.put(
                componentName, getDataStorageScope(scope), getScopeId(scope), key, type, value);

            return null;
        }

        @Override
        public Void remove(Data.Scope scope, String key) {
            dataStorage.delete(
                componentName, getDataStorageScope(scope), getScopeId(scope), key, type);

            return null;
        }

        private DataStorageScope getDataStorageScope(Scope scope) {
            return switch (scope) {
                case WORKFLOW -> DataStorageScope.WORKFLOW;
                case ACCOUNT -> DataStorageScope.ACCOUNT;
            };
        }

        private String getScopeId(Data.Scope scope) {
            return Validate.notNull(
                switch (scope) {
                    case WORKFLOW -> workflowUuid;
                    case ACCOUNT -> null;
                }, "scope");
        }
    }
}
