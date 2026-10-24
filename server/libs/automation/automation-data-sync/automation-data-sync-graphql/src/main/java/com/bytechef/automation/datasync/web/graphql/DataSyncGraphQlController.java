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

package com.bytechef.automation.datasync.web.graphql;

import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.dto.DataSyncDTO;
import com.bytechef.automation.datasync.dto.DataSyncDeploymentDTO;
import com.bytechef.automation.datasync.dto.DataSyncVersionDTO;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for CRUD management of {@code DataSync} entities and their {@link DataSyncElement} building
 * blocks. Every mutation delegates 1:1 to {@link DataSyncFacade}, which owns authorization and the
 * draft-workflow-regeneration side effects — this controller only maps between GraphQL's ID-as-string wire shape and
 * the facade's {@code long}/{@code Long} parameters, and flattens {@link DataSyncDTO} into the {@code DataSync} GraphQL
 * type via {@link DataSyncPayload}.
 *
 * @author Ivica Cardic
 */
@Controller
public class DataSyncGraphQlController {

    private final DataSyncFacade dataSyncFacade;

    @SuppressFBWarnings("EI")
    public DataSyncGraphQlController(DataSyncFacade dataSyncFacade) {
        this.dataSyncFacade = dataSyncFacade;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public DataSyncPayload dataSync(@Argument long id) {
        return new DataSyncPayload(dataSyncFacade.getDataSync(id));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<DataSyncPayload> dataSyncs(@Argument long workspaceId) {
        return dataSyncFacade.getDataSyncs(workspaceId)
            .stream()
            .map(DataSyncPayload::new)
            .toList();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<DataSyncDeploymentDTO> dataSyncDeployments(@Argument long workspaceId) {
        return dataSyncFacade.getDataSyncDeployments(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<DataSyncVersionDTO> dataSyncVersions(@Argument long id) {
        return dataSyncFacade.getDataSyncVersions(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public DataSyncPayload createDataSync(@Argument CreateDataSyncInput input) {
        return new DataSyncPayload(dataSyncFacade.createDataSync(
            input.title(), input.description(), input.workspaceId(), input.projectId()));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public DataSyncPayload updateDataSync(@Argument UpdateDataSyncInput input) {
        return new DataSyncPayload(dataSyncFacade.updateDataSync(input.id(), input.title(), input.description()));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateDataSyncTrigger(@Argument UpdateDataSyncTriggerInput input) {
        dataSyncFacade.updateDataSyncTrigger(input.id(), input.triggerType(), input.triggerParameters());

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public DataSyncElement setDataSyncElement(@Argument SetDataSyncElementInput input) {
        return dataSyncFacade.setDataSyncElement(
            input.dataSyncId(), input.kind(), input.componentName(), input.componentVersion(),
            input.operationName(), input.parameters(), input.connectionId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateDataSyncElement(@Argument UpdateDataSyncElementInput input) {
        dataSyncFacade.updateDataSyncElement(input.id(), input.parameters(), input.connectionId());

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteDataSync(@Argument long id) {
        dataSyncFacade.deleteDataSync(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public long runDataSyncDeployment(@Argument long id, @Argument long projectDeploymentId) {
        return dataSyncFacade.runDataSyncDeployment(id, projectDeploymentId);
    }

    @SuppressFBWarnings("EI")
    public record DataSyncPayload(DataSyncDTO dataSyncDTO) {

        public Long id() {
            return dataSyncDTO.dataSync()
                .getId();
        }

        public String name() {
            return dataSyncDTO.dataSync()
                .getName();
        }

        public String title() {
            return dataSyncDTO.dataSync()
                .getTitle();
        }

        public @Nullable String description() {
            return dataSyncDTO.dataSync()
                .getDescription();
        }

        public @Nullable Long workspaceId() {
            return dataSyncDTO.dataSync()
                .getWorkspaceId();
        }

        public long projectId() {
            return dataSyncDTO.projectId();
        }

        public UUID projectWorkflowUuid() {
            return dataSyncDTO.dataSync()
                .getProjectWorkflowUuid();
        }

        public UUID uuid() {
            return dataSyncDTO.dataSync()
                .getUuid();
        }

        public TriggerType triggerType() {
            return dataSyncDTO.dataSync()
                .getTriggerType();
        }

        public Map<String, ?> triggerParameters() {
            return dataSyncDTO.dataSync()
                .getTriggerParameters();
        }

        public List<DataSyncElement> elements() {
            return dataSyncDTO.elements();
        }

        public boolean unpublishedChanges() {
            return dataSyncDTO.unpublishedChanges();
        }

        public int lastPublishedVersion() {
            return dataSyncDTO.lastPublishedVersion();
        }

        public @Nullable Instant publishedDate() {
            return dataSyncDTO.publishedDate();
        }

        public @Nullable Instant lastModifiedDate() {
            return dataSyncDTO.lastModifiedDate();
        }

        public String draftWorkflowId() {
            return dataSyncDTO.draftWorkflowId();
        }
    }

    public record CreateDataSyncInput(
        String title, @Nullable String description, long workspaceId, @Nullable Long projectId) {
    }

    public record UpdateDataSyncInput(long id, @Nullable String title, @Nullable String description) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record UpdateDataSyncTriggerInput(
        long id, TriggerType triggerType, @Nullable Map<String, Object> triggerParameters) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record SetDataSyncElementInput(
        long dataSyncId, Kind kind, String componentName, int componentVersion, String operationName,
        @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record UpdateDataSyncElementInput(
        long id, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }
}
