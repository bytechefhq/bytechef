/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.configuration.facade.OrganizationConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for Organization Connections.
 *
 * <p>
 * Authorization (ADMIN) is enforced on {@link OrganizationConnectionFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class OrganizationConnectionGraphQlController {

    private final OrganizationConnectionFacade organizationConnectionFacade;

    @SuppressFBWarnings("EI")
    public OrganizationConnectionGraphQlController(OrganizationConnectionFacade organizationConnectionFacade) {
        this.organizationConnectionFacade = organizationConnectionFacade;
    }

    @QueryMapping
    public List<OrganizationConnectionResponse> organizationConnections(@Argument Long environmentId) {
        return organizationConnectionFacade.getOrganizationConnections(environmentId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @MutationMapping
    public long createOrganizationConnection(@Argument CreateOrganizationConnectionInput input) {
        List<TagInput> inputTags = input.tags();

        List<TagInput> tagInputs = inputTags == null ? List.of() : inputTags;

        List<Tag> tags = tagInputs.stream()
            .map(tagInput -> {
                Tag tag = new Tag();

                tag.setId(tagInput.id());
                tag.setName(tagInput.name());

                return tag;
            })
            .toList();

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .name(input.name())
            .authorizationType(input.authorizationType())
            .componentName(input.componentName())
            .connectionVersion(input.connectionVersion())
            .environmentId(input.environmentId())
            .parameters(input.parameters())
            .tags(tags)
            .visibility(ResourceVisibility.ORGANIZATION)
            .build();

        return organizationConnectionFacade.create(connectionDTO);
    }

    @MutationMapping
    public boolean deleteOrganizationConnection(@Argument long connectionId) {
        organizationConnectionFacade.delete(connectionId);

        return true;
    }

    @MutationMapping
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

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record CreateOrganizationConnectionInput(
        String name, @Nullable AuthorizationType authorizationType, String componentName, int connectionVersion,
        int environmentId, Map<String, Object> parameters, @Nullable List<TagInput> tags) {
    }

    public record TagInput(@Nullable Long id, String name) {
    }

    public record OrganizationConnectionResponse(
        Long id, String name, String componentName, int environmentId, String visibility,
        String createdBy, String createdDate, String lastModifiedDate) {
    }
}
