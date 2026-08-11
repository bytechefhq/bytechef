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

package com.bytechef.automation.configuration.audit;

/**
 * Audit event types emitted through {@link ProjectDeploymentAuditPublisher} for project-deployment-lifecycle mutations.
 *
 * @author Ivica Cardic
 */
public enum ProjectDeploymentAuditEvent {

    /**
     * A new project deployment was persisted. Payload: {@code projectDeploymentId} identifies the created row.
     */
    DEPLOYMENT_CREATED,

    /**
     * An existing project deployment was updated. Payload: {@code projectDeploymentId} identifies the updated row.
     */
    DEPLOYMENT_UPDATED,

    /**
     * A project deployment was enabled. Payload: {@code projectDeploymentId} identifies the affected row.
     */
    DEPLOYMENT_ENABLED,

    /**
     * A project deployment was disabled. Payload: {@code projectDeploymentId} identifies the affected row.
     */
    DEPLOYMENT_DISABLED,

    /**
     * A project deployment was deleted. Payload: {@code projectDeploymentId} identifies the now-removed row.
     */
    DEPLOYMENT_DELETED
}
