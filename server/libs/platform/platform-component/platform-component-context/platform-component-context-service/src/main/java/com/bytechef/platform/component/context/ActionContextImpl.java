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

import com.bytechef.atlas.coordinator.event.TaskProgressedApplicationEvent;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Approval.Links;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.ApprovalId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
class ActionContextImpl extends ContextImpl implements ActionContext, ActionContextAware {

    private final String actionName;
    @Nullable
    private Approval approval;
    private final ContextFactory contextFactory;
    private final Data data;
    private final Event event;
    @Nullable
    private final Long jobPrincipalId;
    @Nullable
    private final Long jobPrincipalWorkflowId;
    @Nullable
    private final Long jobId;
    @Nullable
    private final ModeType modeType;
    @Nullable
    private final String workflowId;
    @Nullable
    private final Long environmentId;

    @SuppressFBWarnings("EI")
    public ActionContextImpl(
        String componentName, int componentVersion, String actionName, @Nullable Long jobPrincipalId,
        @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId, @Nullable String workflowId,
        @Nullable ComponentConnection componentConnection, @Nullable String publicUrl, CacheManager cacheManager,
        ContextFactory contextFactory, DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
        HttpClientExecutor httpClientExecutor, TempFileStorage tempFileStorage, @Nullable Long environmentId,
        @Nullable ModeType type, boolean editorEnvironment) {

        super(
            componentName, componentVersion, actionName, componentConnection, editorEnvironment, httpClientExecutor,
            tempFileStorage);

        this.actionName = actionName;
        this.contextFactory = contextFactory;

        if (jobId != null && publicUrl != null) {
            this.approval = new ApprovalImpl(jobId, publicUrl);
        }

        this.data = new DataImpl(
            dataStorage, componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId,
            workflowId, cacheManager, environmentId, type, editorEnvironment);
        this.event = jobId == null ? progress -> {} : new EventImpl(eventPublisher, jobId);
        this.jobPrincipalId = jobPrincipalId;
        this.jobPrincipalWorkflowId = jobPrincipalWorkflowId;
        this.jobId = jobId;
        this.modeType = type;
        this.workflowId = workflowId;
        this.environmentId = environmentId;
    }

    @Override
    public Links approval(ContextFunction<Approval, Links> approvalFunction) {
        try {
            return approvalFunction.apply(approval);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
    public ClusterElementContext createClusterElementContext(
        String componentName, int componentVersion, String componentOperationName,
        @Nullable ComponentConnection componentConnection) {

        return contextFactory.createClusterElementContext(
            componentName, componentVersion, componentOperationName, componentConnection, isEditorEnvironment());
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    @Override
    @Nullable
    public Long getJobPrincipalId() {
        return jobPrincipalId;
    }

    @Override
    @Nullable
    public Long getJobPrincipalWorkflowId() {
        return jobPrincipalWorkflowId;
    }

    @Override
    @Nullable
    public Long getJobId() {
        return jobId;
    }

    @Override
    @Nullable
    public ModeType getModeType() {
        return modeType;
    }

    @Override
    @Nullable
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    @Nullable
    public Long getEnvironmentId() {
        return environmentId;
    }

    private record ApprovalImpl(long jobId, String publicUrl) implements Approval {

        @Override
        public Links generateLinks() {
            String url = "%s/approvals/%s";

            return new Links(
                url.formatted(publicUrl, ApprovalId.of(jobId, true)),
                url.formatted(publicUrl, ApprovalId.of(jobId, false)));
        }
    }

    private static final class DataImpl implements Data {

        private final String actionName;
        private final String componentName;
        private final Integer componentVersion;
        private final DataStorage dataStorage;
        private final boolean editorEnvironment;
        @Nullable
        private final Long environmentId;
        private final InMemoryDataStorage inMemoryDataStorage;
        @Nullable
        private final Long jobPrincipalId;
        @Nullable
        private final Long jobPrincipalWorkflowId;
        @Nullable
        private final Long jobId;
        @Nullable
        private final ModeType type;

        private DataImpl(
            DataStorage dataStorage, String componentName, Integer componentVersion, String actionName,
            @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId,
            @Nullable String workflowId, CacheManager cacheManager, @Nullable Long environmentId,
            @Nullable ModeType type, boolean editorEnvironment) {

            this.actionName = actionName;
            this.componentName = componentName;
            this.componentVersion = componentVersion;
            this.dataStorage = dataStorage;
            this.editorEnvironment = editorEnvironment;
            this.environmentId = environmentId;
            this.inMemoryDataStorage = new InMemoryDataStorage(workflowId, cacheManager);
            this.jobId = jobId;
            this.jobPrincipalId = jobPrincipalId;
            this.jobPrincipalWorkflowId = jobPrincipalWorkflowId;
            this.type = type;
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
        public <T> Map<String, T> getAll(Scope scope) {
            if (editorEnvironment) {
                return inMemoryDataStorage.getAll(componentName, getDataStorageScope(scope), getScopeId(scope));
            }

            return dataStorage.getAll(
                componentName, getDataStorageScope(scope), getScopeId(scope), Objects.requireNonNull(environmentId),
                Objects.requireNonNull(type));
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
                case CURRENT_EXECUTION -> DataStorageScope.CURRENT_EXECUTION;
                case WORKFLOW -> DataStorageScope.WORKFLOW;
                case PRINCIPAL -> DataStorageScope.PRINCIPAL;
                case ACCOUNT -> DataStorageScope.ACCOUNT;
            };
        }

        private String getScopeId(Scope scope) {
            return Validate.notNull(
                switch (scope) {
                    case CURRENT_EXECUTION -> String.valueOf(jobId);
                    case WORKFLOW -> String.valueOf(jobPrincipalWorkflowId);
                    case PRINCIPAL -> String.valueOf(jobPrincipalId);
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

        public String actionName() {
            return actionName;
        }

        @Nullable
        public Long jobPrincipalId() {
            return jobPrincipalId;
        }

        @Nullable
        public Long jobPrincipalWorkflowId() {
            return jobPrincipalWorkflowId;
        }

        @Nullable
        public Long jobId() {
            return jobId;
        }

        @Nullable
        public Long environmentId() {
            return environmentId;
        }

        @Nullable
        public ModeType type() {
            return type;
        }

        public boolean editorEnvironment() {
            return editorEnvironment;
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
                Objects.equals(this.actionName, that.actionName) &&
                Objects.equals(this.jobPrincipalId, that.jobPrincipalId) &&
                Objects.equals(this.jobPrincipalWorkflowId, that.jobPrincipalWorkflowId) &&
                Objects.equals(this.jobId, that.jobId) &&
                Objects.equals(this.environmentId, that.environmentId) &&
                Objects.equals(this.type, that.type) &&
                this.editorEnvironment == that.editorEnvironment;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataStorage, componentName, componentVersion, actionName, jobPrincipalId,
                jobPrincipalWorkflowId, jobId, environmentId, type, editorEnvironment);
        }

        @Override
        public String toString() {
            return "DataImpl[" +
                "dataStorage=" + dataStorage + ", " +
                "componentName=" + componentName + ", " +
                "componentVersion=" + componentVersion + ", " +
                "actionName=" + actionName + ", " +
                "jobPrincipalId=" + jobPrincipalId + ", " +
                "jobPrincipalWorkflowId=" + jobPrincipalWorkflowId + ", " +
                "jobId=" + jobId + ", " +
                "environmentId=" + environmentId + ", " +
                "type=" + type + ", " +
                "editorEnvironment=" + editorEnvironment + ']';
        }

    }

    private record EventImpl(ApplicationEventPublisher eventPublisher, long taskExecutionId) implements Event {

        @Override
        public void publishActionProgressEvent(int progress) {
            eventPublisher.publishEvent(new TaskProgressedApplicationEvent(taskExecutionId, progress));
        }
    }
}
