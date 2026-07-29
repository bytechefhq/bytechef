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

package com.bytechef.platform.tool.execution;

import org.jspecify.annotations.Nullable;

/**
 * An immutable record of a single direct tool/action execution. Carries identity and outcome metadata only — never the
 * invocation's input parameters or output payload.
 *
 * @author Ivica Cardic
 */
public record ToolExecutionEvent(
    @Nullable String tenantId, ToolExecutionSurface surface, ToolExecutionKind kind, String toolName,
    @Nullable String componentName, @Nullable Integer componentVersion, @Nullable String operationName,
    @Nullable Long connectionId, @Nullable Integer environment, @Nullable Long workspaceId,
    @Nullable String externalUserId, @Nullable Long connectedUserId, @Nullable Long integrationInstanceId,
    @Nullable Long mcpServerId, @Nullable Long jobId, ToolExecutionOutcome outcome, @Nullable String errorType,
    @Nullable String errorMessage, long durationMs) {

    public static final int ERROR_MESSAGE_MAX_LENGTH = 512;

    public static Builder builder(ToolExecutionSurface surface, ToolExecutionKind kind, String toolName) {
        return new Builder(surface, kind, toolName);
    }

    /**
     * Mutable builder for {@link ToolExecutionEvent}. Identity fields are set at the call site; outcome, error and
     * duration are filled in by the {@link ToolExecutionRecorder} once the execution completes.
     */
    public static final class Builder {

        private final ToolExecutionSurface surface;
        private final ToolExecutionKind kind;
        private final String toolName;

        private @Nullable String tenantId;
        private @Nullable String componentName;
        private @Nullable Integer componentVersion;
        private @Nullable String operationName;
        private @Nullable Long connectionId;
        private @Nullable Integer environment;
        private @Nullable Long workspaceId;
        private @Nullable String externalUserId;
        private @Nullable Long connectedUserId;
        private @Nullable Long integrationInstanceId;
        private @Nullable Long mcpServerId;
        private @Nullable Long jobId;
        private ToolExecutionOutcome outcome = ToolExecutionOutcome.SUCCESS;
        private @Nullable String errorType;
        private @Nullable String errorMessage;
        private long durationMs;

        private Builder(ToolExecutionSurface surface, ToolExecutionKind kind, String toolName) {
            this.surface = surface;
            this.kind = kind;
            this.toolName = toolName;
        }

        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;

            return this;
        }

        public Builder componentName(@Nullable String componentName) {
            this.componentName = componentName;

            return this;
        }

        public Builder componentVersion(@Nullable Integer componentVersion) {
            this.componentVersion = componentVersion;

            return this;
        }

        public Builder operationName(@Nullable String operationName) {
            this.operationName = operationName;

            return this;
        }

        public Builder connectionId(@Nullable Long connectionId) {
            this.connectionId = connectionId;

            return this;
        }

        public Builder environment(@Nullable Integer environment) {
            this.environment = environment;

            return this;
        }

        public Builder workspaceId(@Nullable Long workspaceId) {
            this.workspaceId = workspaceId;

            return this;
        }

        public Builder externalUserId(@Nullable String externalUserId) {
            this.externalUserId = externalUserId;

            return this;
        }

        public Builder connectedUserId(@Nullable Long connectedUserId) {
            this.connectedUserId = connectedUserId;

            return this;
        }

        public Builder integrationInstanceId(@Nullable Long integrationInstanceId) {
            this.integrationInstanceId = integrationInstanceId;

            return this;
        }

        public Builder mcpServerId(@Nullable Long mcpServerId) {
            this.mcpServerId = mcpServerId;

            return this;
        }

        public Builder jobId(@Nullable Long jobId) {
            this.jobId = jobId;

            return this;
        }

        public Builder outcome(ToolExecutionOutcome outcome) {
            this.outcome = outcome;

            return this;
        }

        public Builder errorType(@Nullable String errorType) {
            this.errorType = errorType;

            return this;
        }

        public Builder errorMessage(@Nullable String errorMessage) {
            if (errorMessage != null && errorMessage.length() > ERROR_MESSAGE_MAX_LENGTH) {
                errorMessage = errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
            }

            this.errorMessage = errorMessage;

            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;

            return this;
        }

        public ToolExecutionEvent build() {
            return new ToolExecutionEvent(
                tenantId, surface, kind, toolName, componentName, componentVersion, operationName, connectionId,
                environment, workspaceId, externalUserId, connectedUserId, integrationInstanceId, mcpServerId, jobId,
                outcome, errorType, errorMessage, durationMs);
        }
    }
}
