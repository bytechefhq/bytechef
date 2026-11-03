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

package com.bytechef.platform.workflow.execution.accessor;

import org.jspecify.annotations.Nullable;

/**
 * Describes the principal a job runs under, for notifications. For {@code EMBEDDED} jobs the {@code project*} fields
 * describe the integration.
 *
 * @param environmentId  the environment identifier (ordinal of the platform environment)
 * @param projectId      the project id, or the integration id for embedded jobs
 * @param projectName    the project name, or the integration name for embedded jobs
 * @param projectVersion the deployed project version, or the integration version for embedded jobs
 *
 * @author Ivica Cardic
 */
public record JobPrincipalSummary(
    long environmentId, @Nullable Long projectId, @Nullable String projectName, @Nullable Integer projectVersion) {
}
