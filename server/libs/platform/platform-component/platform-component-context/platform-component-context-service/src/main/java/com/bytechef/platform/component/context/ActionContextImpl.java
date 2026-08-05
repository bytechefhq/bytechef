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
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Approval.Links;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.log.LogFileStorageWriter;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ActionContextImpl extends ContextImpl implements ActionContext, ActionContextAware {

    private static final Logger log = LoggerFactory.getLogger(ActionContextImpl.class);

    private final String actionName;
    private @Nullable Approval approval;
    private final @Nullable ApprovalTokens approvalTokens;
    private final CacheManager cacheManager;
    private final Data data;
    private final DataStorage dataStorage;
    private final boolean editorEnvironment;
    private final Event event;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpClientExecutor httpClientExecutor;
    private final @Nullable Long jobPrincipalId;
    private final @Nullable Long jobPrincipalWorkflowId;
    private final @Nullable Long jobId;
    private final @Nullable JobService jobService;
    /**
     * Phase 17b: cached parent-job metadata, lazily resolved on the first {@link #getJobMetadata()} call. Null until
     * first call; empty map after a failed/skipped lookup. The two states cannot be merged because the lookup is
     * genuinely lazy — pre-call we have no information; post-call we have "no metadata available".
     */
    private @Nullable Map<String, Object> jobMetadata;
    private final @Nullable LogFileStorageWriter logFileStorageWriter;
    private @Nullable String jobResumeId;
    private @Nullable Suspend suspend;
    private final @Nullable PlatformType type;
    private final @Nullable String publicUrl;
    private final long taskExecutionId;
    private final TempFileStorage tempFileStorage;
    private final @Nullable Tracer tracer;
    private final @Nullable String workflowId;
    private final @Nullable Long environmentId;

    @SuppressFBWarnings("EI")
    private ActionContextImpl(Builder builder) {
        super(
            builder.componentName, builder.componentVersion, builder.actionName, builder.componentConnection,
            builder.jobId, builder.taskExecutionId, builder.editorEnvironment, builder.httpClientExecutor,
            builder.tempFileStorage, builder.logFileStorageWriter);

        this.actionName = builder.actionName;
        this.approvalTokens = builder.approvalTokens;
        this.cacheManager = builder.cacheManager;
        this.dataStorage = builder.dataStorage;
        this.editorEnvironment = builder.editorEnvironment;
        this.eventPublisher = builder.eventPublisher;
        this.httpClientExecutor = builder.httpClientExecutor;
        this.logFileStorageWriter = builder.logFileStorageWriter;
        this.taskExecutionId = builder.taskExecutionId;
        this.tempFileStorage = builder.tempFileStorage;
        this.tracer = builder.tracer;

        if (builder.jobId != null && builder.publicUrl != null) {
            this.approval = new ApprovalImpl(builder.jobId, builder.publicUrl, builder.approvalTokens);
        }

        this.data = new DataImpl(
            builder.dataStorage, builder.componentName, builder.componentVersion, builder.actionName,
            builder.jobPrincipalId, builder.jobPrincipalWorkflowId, builder.jobId, builder.workflowId,
            builder.cacheManager, builder.environmentId, builder.type, builder.editorEnvironment);
        this.environmentId = builder.environmentId;
        this.event = builder.jobId == null
            ? progress -> {}
            : new EventImpl(builder.eventPublisher, builder.jobId);
        this.jobPrincipalId = builder.jobPrincipalId;
        this.jobPrincipalWorkflowId = builder.jobPrincipalWorkflowId;
        this.jobId = builder.jobId;
        this.jobService = builder.jobService;
        this.publicUrl = builder.publicUrl;
        this.type = builder.type;
        this.workflowId = builder.workflowId;
    }

    static Builder builder(
        String componentName, int componentVersion, String actionName, boolean editorEnvironment,
        CacheManager cacheManager, DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
        HttpClientExecutor httpClientExecutor, TempFileStorage tempFileStorage,
        @Nullable JobService jobService, @Nullable ApprovalTokens approvalTokens) {

        return new Builder(
            componentName, componentVersion, actionName, editorEnvironment, cacheManager, dataStorage, eventPublisher,
            httpClientExecutor, tempFileStorage, jobService, approvalTokens);
    }

    @Override
    public ActionContext toActionContext(
        String componentName, int componentVersion, String actionName,
        @Nullable ComponentConnection componentConnection) {

        return builder(
            componentName, componentVersion, actionName, editorEnvironment, cacheManager, dataStorage,
            eventPublisher, httpClientExecutor, tempFileStorage, jobService, approvalTokens)
                .componentConnection(componentConnection)
                .environmentId(environmentId)
                .jobId(jobId)
                .jobPrincipalId(jobPrincipalId)
                .jobPrincipalWorkflowId(jobPrincipalWorkflowId)
                .logFileStorageWriter(logFileStorageWriter)
                .publicUrl(publicUrl)
                .taskExecutionId(taskExecutionId)
                .type(type)
                .workflowId(workflowId)
                .build();
    }

    private String toResumeToken(String innerToken) {
        if (approvalTokens == null) {
            return innerToken;
        }

        return approvalTokens.toSignedTokenIfConfigured(innerToken)
            .orElse(innerToken);
    }

    static final class Builder {

        private final String actionName;
        private final @Nullable ApprovalTokens approvalTokens;
        private final CacheManager cacheManager;
        private @Nullable ComponentConnection componentConnection;
        private final String componentName;
        private final int componentVersion;
        private final DataStorage dataStorage;
        private final boolean editorEnvironment;
        private @Nullable Long environmentId;
        private final ApplicationEventPublisher eventPublisher;
        private final HttpClientExecutor httpClientExecutor;
        private @Nullable Long jobId;
        private @Nullable Long jobPrincipalId;
        private @Nullable Long jobPrincipalWorkflowId;
        private final @Nullable JobService jobService;
        private @Nullable LogFileStorageWriter logFileStorageWriter;
        private @Nullable String publicUrl;
        private long taskExecutionId;
        private @Nullable Tracer tracer;
        private final TempFileStorage tempFileStorage;
        private @Nullable PlatformType type;
        private @Nullable String workflowId;

        private Builder(
            String componentName, int componentVersion, String actionName, boolean editorEnvironment,
            CacheManager cacheManager, DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
            HttpClientExecutor httpClientExecutor, TempFileStorage tempFileStorage,
            @Nullable JobService jobService, @Nullable ApprovalTokens approvalTokens) {

            this.componentName = componentName;
            this.componentVersion = componentVersion;
            this.actionName = actionName;
            this.editorEnvironment = editorEnvironment;
            this.cacheManager = cacheManager;
            this.dataStorage = dataStorage;
            this.eventPublisher = eventPublisher;
            this.httpClientExecutor = httpClientExecutor;
            this.tempFileStorage = tempFileStorage;
            this.jobService = jobService;
            this.approvalTokens = approvalTokens;
        }

        Builder componentConnection(@Nullable ComponentConnection componentConnection) {
            this.componentConnection = componentConnection;

            return this;
        }

        Builder tracer(@Nullable Tracer tracer) {
            this.tracer = tracer;

            return this;
        }

        Builder environmentId(@Nullable Long environmentId) {
            this.environmentId = environmentId;

            return this;
        }

        Builder jobId(@Nullable Long jobId) {
            this.jobId = jobId;

            return this;
        }

        Builder jobPrincipalId(@Nullable Long jobPrincipalId) {
            this.jobPrincipalId = jobPrincipalId;

            return this;
        }

        Builder jobPrincipalWorkflowId(@Nullable Long jobPrincipalWorkflowId) {
            this.jobPrincipalWorkflowId = jobPrincipalWorkflowId;

            return this;
        }

        Builder logFileStorageWriter(@Nullable LogFileStorageWriter logFileStorageWriter) {
            this.logFileStorageWriter = logFileStorageWriter;

            return this;
        }

        Builder publicUrl(@Nullable String publicUrl) {
            this.publicUrl = publicUrl;

            return this;
        }

        Builder taskExecutionId(long taskExecutionId) {
            this.taskExecutionId = taskExecutionId;

            return this;
        }

        Builder type(@Nullable PlatformType type) {
            this.type = type;

            return this;
        }

        Builder workflowId(@Nullable String workflowId) {
            this.workflowId = workflowId;

            return this;
        }

        ActionContextImpl build() {
            return new ActionContextImpl(this);
        }
    }

    @Override
    public String getTraceId() {
        if (tracer == null) {
            return UUID.randomUUID()
                .toString();
        }

        TraceContext context = tracer.currentTraceContext()
            .context();

        if (context == null) {
            return UUID.randomUUID()
                .toString();
        }

        return context.traceId();
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
    public String getActionName() {
        return actionName;
    }

    @Override
    @Nullable
    public String getResumeUrl() {
        if (publicUrl == null || jobId == null) {
            return null;
        }

        if (this.jobResumeId == null) {
            JobResumeId jobResumeId = JobResumeId.of(jobId);

            this.jobResumeId = jobResumeId.toString();
        }

        // The stored JOB_RESUME_ID metadata (getJobResumeId) stays the unsigned inner value for the stored-uuid match;
        // only the URL carries the HMAC-signed wrapper.
        return publicUrl + "/job/resume/" + toResumeToken(this.jobResumeId);
    }

    @Override
    @Nullable
    public String getJobResumeId() {
        return jobResumeId;
    }

    @Override
    @Nullable
    public Suspend getSuspend() {
        return suspend;
    }

    @Override
    public void suspend(Suspend suspend) {
        this.suspend = suspend;
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

    /**
     * Narrows a {@code Map<String, ?>} value-wildcard map into a defensively-copied {@code Map<String, Object>}.
     * Required because {@code Job.getMetadata()} declares its return type with a wildcard (a "don't mutate me" marker
     * for callers), but {@link ActionContextAware#getJobMetadata()} commits to {@code Map<String, Object>}. Defensive
     * copy detaches the cached view from future Job mutations.
     */
    private static Map<String, Object> narrowToObjectMap(@Nullable Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        HashMap<String, Object> copy = new HashMap<>(source.size());

        source.forEach(copy::put);

        return Collections.unmodifiableMap(copy);
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, Object> getJobMetadata() {
        if (jobMetadata != null) {
            return jobMetadata;
        }

        // Editor-environment runs and direct in-process invocations have no persisted Atlas Job; pre-17b
        // workflows similarly never carried __jobParameters. In all these cases the dataStream action reads an
        // empty map and falls back to its task-level baked parameters — same behavior as before Phase 17b.
        if (jobId == null || jobService == null) {
            jobMetadata = Map.of();

            return jobMetadata;
        }

        try {
            jobMetadata = jobService.fetchJob(jobId)
                .map(job -> narrowToObjectMap(job.getMetadata()))
                .orElseGet(Map::of);
        } catch (RuntimeException ex) {
            // A transient lookup failure can't be allowed to break the action's perform path. The reader-side
            // contract treats an empty map identically to "no override" — same recovery as the missing-job
            // case above.
            log.warn("Failed to load job {} metadata for ActionContextAware.getJobMetadata: {}",
                jobId, ex.getMessage());

            jobMetadata = Map.of();
        }

        return jobMetadata;
    }

    @Override
    @Nullable
    public Long getParentTaskExecutionId() {
        // No persisted Atlas Job in editor-environment / in-process invocations -- treat as top-level.
        if (jobId == null || jobService == null) {
            return null;
        }

        try {
            return jobService.fetchJob(jobId)
                .map(Job::getParentTaskExecutionId)
                .orElse(null);
        } catch (RuntimeException ex) {
            // Mirror the recovery shape of getJobMetadata: a transient lookup failure must not break perform().
            // The reader (WorkflowCallWorkflowTool) interprets null as "top-level", which is the safe direction --
            // the guard fails open (suspend proceeds) rather than closed (block a legitimate top-level agent).
            log.warn(
                "Failed to load job {} for ActionContextAware.getParentTaskExecutionId: {}", jobId, ex.getMessage());

            return null;
        }
    }

    @Override
    @Nullable
    public PlatformType getPlatformType() {
        return type;
    }

    @Override
    @Nullable
    public String getPublicUrl() {
        return publicUrl;
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

    @Override
    public boolean isEditorEnvironment() {
        return editorEnvironment;
    }

    @Override
    public ClusterElementContext toClusterElementContext(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ComponentConnection componentConnection) {

        return ClusterElementContextImpl
            .builder(
                componentName, componentVersion, clusterElementName, editorEnvironment, cacheManager, dataStorage,
                eventPublisher, httpClientExecutor, tempFileStorage)
            .componentConnection(componentConnection)
            .environmentId(environmentId)
            .jobId(jobId)
            .jobPrincipalId(jobPrincipalId)
            .jobPrincipalWorkflowId(jobPrincipalWorkflowId)
            .logFileStorageWriter(logFileStorageWriter)
            .publicUrl(publicUrl)
            .taskExecutionId(taskExecutionId)
            .type(type)
            .workflowId(workflowId)
            .build();
    }

    private record ApprovalImpl(long jobId, String publicUrl, @Nullable ApprovalTokens approvalTokens)
        implements Approval {

        @Override
        public Links generateLinks() {
            String url = "%s/approvals/%s";

            return new Links(
                url.formatted(publicUrl, toApprovalToken(ApprovalId.of(jobId, true))),
                url.formatted(publicUrl, toApprovalToken(ApprovalId.of(jobId, false))));
        }

        private String toApprovalToken(ApprovalId approvalId) {
            String innerToken = approvalId.toString();

            if (approvalTokens == null) {
                return innerToken;
            }

            return approvalTokens.toSignedTokenIfConfigured(innerToken)
                .orElse(innerToken);
        }
    }

    private static final class DataImpl implements Data {

        private final String actionName;
        private final String componentName;
        private final Integer componentVersion;
        private final DataStorage dataStorage;
        private final boolean editorEnvironment;
        private final @Nullable Long environmentId;
        private final InMemoryDataStorage inMemoryDataStorage;
        private final @Nullable Long jobPrincipalId;
        private final @Nullable Long jobPrincipalWorkflowId;
        private final @Nullable Long jobId;
        private final @Nullable PlatformType type;

        private DataImpl(
            DataStorage dataStorage, String componentName, Integer componentVersion, String actionName,
            @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId,
            @Nullable String workflowId, CacheManager cacheManager, @Nullable Long environmentId,
            @Nullable PlatformType type, boolean editorEnvironment) {

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

        public @Nullable Long jobPrincipalId() {
            return jobPrincipalId;
        }

        public @Nullable Long jobPrincipalWorkflowId() {
            return jobPrincipalWorkflowId;
        }

        public @Nullable Long jobId() {
            return jobId;
        }

        public @Nullable Long environmentId() {
            return environmentId;
        }

        public @Nullable PlatformType type() {
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
