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
 * @author Ivica Cardic
 */
public final class ProjectDeploymentAuditEvents {

    public static final String DEPLOYMENT_CREATED = "DEPLOYMENT_CREATED";
    public static final String DEPLOYMENT_DELETED = "DEPLOYMENT_DELETED";
    public static final String DEPLOYMENT_ENABLED_UPDATED = "DEPLOYMENT_ENABLED_UPDATED";
    public static final String DEPLOYMENT_UPDATED = "DEPLOYMENT_UPDATED";
    public static final String DEPLOYMENT_WORKFLOW_ENABLED_UPDATED = "DEPLOYMENT_WORKFLOW_ENABLED_UPDATED";
    public static final String DEPLOYMENT_WORKFLOW_RUN = "DEPLOYMENT_WORKFLOW_RUN";

    private ProjectDeploymentAuditEvents() {
    }
}
