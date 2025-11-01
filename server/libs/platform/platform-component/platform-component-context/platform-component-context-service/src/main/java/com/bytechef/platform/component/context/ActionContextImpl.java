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
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ActionContextImpl extends ContextImpl implements ActionContext, ActionContextAware {

    private final String actionName;
    private Approval approval;
    private final ContextFactory contextFactory;
    private final Data data;
    private final Event event;
    private final Long jobPrincipalId;
    private final Long jobPrincipalWorkflowId;
    private final Long jobId;
    private final ModeType modeType;
    private final String workflowId;

    @SuppressFBWarnings("EI")
    public ActionContextImpl(
        String actionName, String componentName, int componentVersion,
        @Nullable ComponentConnection componentConnection, ContextFactory contextFactory, DataStorage dataStorage,
        boolean editorEnvironment, ApplicationEventPublisher eventPublisher, HttpClientExecutor httpClientExecutor,
        @Nullable Long jobId, @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId,
        @Nullable ModeType modeType, @Nullable String publicUrl, TempFileStorage tempFileStorage,
        @Nullable String workflowId) {

        super(
            componentName, componentVersion, actionName, componentConnection, editorEnvironment, httpClientExecutor,
            tempFileStorage);

        this.actionName = actionName;
        this.contextFactory = contextFactory;

        if (jobId != null) {
            this.approval = new ApprovalImpl(jobId, publicUrl);
        }

        this.data = new DataImpl(
            componentName, componentVersion, actionName, modeType, jobPrincipalId, jobPrincipalWorkflowId, jobId,
            dataStorage);
        this.event = jobId == null ? progress -> {} : new EventImpl(eventPublisher, jobId);
        this.jobPrincipalId = jobPrincipalId;
        this.jobPrincipalWorkflowId = jobPrincipalWorkflowId;
        this.jobId = jobId;
        this.modeType = modeType;
        this.workflowId = workflowId;
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
    public Long getJobPrincipalId() {
        return jobPrincipalId;
    }

    @Override
    public Long getJobPrincipalWorkflowId() {
        return jobPrincipalWorkflowId;
    }

    @Override
    public Long getJobId() {
        return jobId;
    }

    @Override
    public ModeType getModeType() {
        return modeType;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
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

    private record DataImpl(
        String componentName, Integer componentVersion, String actionName, ModeType type, Long principalId,
        Long principalWorkflowId, Long jobId, DataStorage dataStorage) implements Data {

        @Override
        public <T> Optional<T> fetch(Scope scope, String key) {
            return dataStorage.fetch(componentName, getDataStorageScope(scope), getScopeId(scope), key, type);
        }

        @Override
        public <T> T get(Scope scope, String key) {
            return dataStorage.get(componentName, getDataStorageScope(scope), getScopeId(scope), key, type);
        }

        @Override
        public <T> Map<String, T> getAll(Scope scope) {
            return dataStorage.getAll(componentName, getDataStorageScope(scope), getScopeId(scope), type);
        }

        @Override
        public Void put(Scope scope, String key, Object value) {
            dataStorage.put(componentName, getDataStorageScope(scope), getScopeId(scope), key, type, value);

            return null;
        }

        @Override
        public Void remove(Scope scope, String key) {
            dataStorage.delete(componentName, getDataStorageScope(scope), getScopeId(scope), key, type);

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
                    case WORKFLOW -> String.valueOf(principalWorkflowId);
                    case PRINCIPAL -> String.valueOf(principalId);
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
}
