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

package com.bytechef.automation.datasync.dto;

import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Row of the Data Sync deployments listing: one {@code project_deployment} of a Data Sync's project whose deployed
 * version contains the Data Sync's generated workflow, flattened with the fields the listing needs from the owning
 * {@link com.bytechef.automation.datasync.domain.DataSync} so the client does not have to join them itself. A project
 * can hold several deployments per environment, so a Data Sync can have several rows in one environment.
 *
 * @param id                the {@code project_deployment} id
 * @param name              the deployment's name
 * @param dataSyncId        the owning Data Sync's id
 * @param dataSyncTitle     the owning Data Sync's title
 * @param projectId         the Data Sync's project id
 * @param environmentId     the {@link com.bytechef.platform.configuration.domain.Environment} ordinal this deployment
 *                          targets
 * @param enabled           whether the deployment itself is enabled
 * @param projectVersion    the deployed project version
 * @param workflowId        the id of the Data Sync's workflow in the deployed version
 * @param triggerType       the trigger of the deployed workflow, not of the Data Sync's current row
 * @param lastExecutionDate when the Data Sync's deployed workflow last ran, or {@code null} if it never has
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public record DataSyncDeploymentDTO(
    long id, String name, long dataSyncId, String dataSyncTitle, long projectId, int environmentId, boolean enabled,
    int projectVersion, String workflowId, TriggerType triggerType, @Nullable Instant lastExecutionDate) {
}
