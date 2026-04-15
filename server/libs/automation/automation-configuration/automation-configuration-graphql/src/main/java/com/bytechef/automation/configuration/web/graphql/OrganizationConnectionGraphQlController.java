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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.facade.OrganizationConnectionFacade;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for Organization Connections.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class OrganizationConnectionGraphQlController {

    private final OrganizationConnectionFacade organizationConnectionFacade;

    @SuppressFBWarnings("EI")
    public OrganizationConnectionGraphQlController(OrganizationConnectionFacade organizationConnectionFacade) {
        this.organizationConnectionFacade = organizationConnectionFacade;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<OrganizationConnectionResponse> organizationConnections(@Argument Long environmentId) {
        return organizationConnectionFacade.getOrganizationConnections(environmentId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public long createOrganizationConnection(@Argument CreateOrganizationConnectionInput input) {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .name(input.name())
            .componentName(input.componentName())
            .connectionVersion(input.connectionVersion())
            .environmentId(input.environmentId())
            .parameters(input.parameters())
            .visibility(ConnectionVisibility.ORGANIZATION)
            .build();

        return organizationConnectionFacade.create(connectionDTO);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteOrganizationConnection(@Argument long connectionId) {
        organizationConnectionFacade.delete(connectionId);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean updateOrganizationConnection(
        @Argument long connectionId, @Argument String name,
        @Argument List<Long> tagIds, @Argument int version) {

        organizationConnectionFacade.update(connectionId, name, tagIds != null ? tagIds : List.of(), version);

        return true;
    }

    private OrganizationConnectionResponse toResponse(ConnectionDTO connectionDTO) {
        return new OrganizationConnectionResponse(
            connectionDTO.id(),
            connectionDTO.name(),
            connectionDTO.componentName(),
            connectionDTO.environmentId(),
            connectionDTO.visibility()
                .name(),
            connectionDTO.createdBy(),
            connectionDTO.createdDate() != null ? connectionDTO.createdDate()
                .toString() : null,
            connectionDTO.lastModifiedDate() != null ? connectionDTO.lastModifiedDate()
                .toString() : null);
    }

    public record CreateOrganizationConnectionInput(
        String name, String componentName, int connectionVersion, int environmentId,
        Map<String, Object> parameters) {
    }

    public record OrganizationConnectionResponse(
        Long id, String name, String componentName, int environmentId, String visibility,
        String createdBy, String createdDate, String lastModifiedDate) {
    }
}
