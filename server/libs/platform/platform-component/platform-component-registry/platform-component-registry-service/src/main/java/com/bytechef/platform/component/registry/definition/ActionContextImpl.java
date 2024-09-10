/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.definition;

import com.bytechef.atlas.coordinator.event.TaskProgressedApplicationEvent;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.file.storage.FilesFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class ActionContextImpl extends ContextImpl implements ActionContext, ActionContextAware {

    private final Data data;
    private final Event event;
    private final String actionName;
    private final AppType type;
    private final Long instanceId;
    private final Long instanceWorkflowId;
    private final Long jobId;

    @SuppressFBWarnings("EI")
    public ActionContextImpl(
        String componentName, int componentVersion, String actionName, AppType type,
        Long instanceId, Long instanceWorkflowId, Long jobId, ComponentConnection connection,
        DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
        FilesFileStorage filesFileStorage, HttpClientExecutor httpClientExecutor) {

        super(componentName, componentVersion, actionName, filesFileStorage, connection, httpClientExecutor);

        this.actionName = actionName;
        this.type = type;
        this.instanceId = instanceId;
        this.instanceWorkflowId = instanceWorkflowId;
        this.jobId = jobId;

        if (type == null || instanceId == null || instanceWorkflowId == null || jobId == null) {
            this.data = new NoOpDataImpl();
        } else {
            this.data = new DataImpl(
                componentName, componentVersion, actionName, type, instanceId, instanceWorkflowId, jobId,
                dataStorage);
        }

        this.event = jobId == null ? progress -> {} : new EventImpl(eventPublisher, jobId);
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
    public void event(Consumer<Event> eventConsumer) {
        eventConsumer.accept(event);
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    public AppType getAppType() {
        return type;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public Long getInstanceWorkflowId() {
        return instanceWorkflowId;
    }

    public Long getJobId() {
        return jobId;
    }

    private record DataImpl(
        String componentName, Integer componentVersion, String actionName, AppType type, long instanceId,
        long instanceWorkflowId, long jobId, DataStorage dataStorage) implements Data {

        @Override
        public <T> Optional<T> fetchValue(Scope scope, String key) {
            return dataStorage.fetch(componentName, getDataStorageScope(scope), getScopeId(scope), key, type);
        }

        @Override
        public <T> T getValue(Scope scope, String key) {
            return dataStorage.get(componentName, getDataStorageScope(scope), getScopeId(scope), key, type);
        }

        @Override
        public <T> Map<String, T> getAll(Scope scope) {
            return dataStorage.getAll(componentName, getDataStorageScope(scope), getScopeId(scope), type);
        }

        @Override
        public Void setValue(Scope scope, String key, Object value) {
            dataStorage.put(componentName, getDataStorageScope(scope), getScopeId(scope), key, type, value);

            return null;
        }

        @Override
        public Void deleteValue(Scope scope, String key) {
            dataStorage.delete(componentName, getDataStorageScope(scope), getScopeId(scope), key, type);

            return null;
        }

        private DataStorageScope getDataStorageScope(Scope scope) {
            return switch (scope) {
                case CURRENT_EXECUTION -> DataStorageScope.CURRENT_EXECUTION;
                case WORKFLOW -> DataStorageScope.WORKFLOW;
                case INSTANCE -> DataStorageScope.INSTANCE;
                case ACCOUNT -> DataStorageScope.ACCOUNT;
            };
        }

        private String getScopeId(Scope scope) {
            return Validate.notNull(
                switch (scope) {
                    case CURRENT_EXECUTION -> String.valueOf(jobId);
                    case WORKFLOW -> String.valueOf(instanceWorkflowId);
                    case INSTANCE -> String.valueOf(instanceId);
                    case ACCOUNT -> "";
                }, "scope");
        }
    }

    private record EventImpl(ApplicationEventPublisher eventPublisher, long taskExecutionId) implements Event {

        @Override
        public void publishActionProgressEvent(int progress) {
            eventPublisher.publishEvent(new TaskProgressedApplicationEvent(taskExecutionId, progress));
        }
    }

    private record NoOpDataImpl() implements Data {

        @Override
        public <T> Optional<T> fetchValue(Scope scope, String key) {
            return Optional.empty();
        }

        @Override
        public <T> T getValue(Scope scope, String key) {
            return null;
        }

        @Override
        public <T> Map<String, T> getAll(Scope scope) {
            return null;
        }

        @Override
        public Void setValue(Scope scope, String key, Object data) {
            return null;
        }

        @Override
        public Void deleteValue(Scope scope, String key) {
            return null;
        }
    }
}
