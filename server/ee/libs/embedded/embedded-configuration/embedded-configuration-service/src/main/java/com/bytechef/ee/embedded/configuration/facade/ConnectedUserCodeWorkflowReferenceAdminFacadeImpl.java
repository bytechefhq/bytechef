/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserCodeWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Admin-only read seam over automation-bridge references, joined back to the connected user each reference belongs to
 * -- the direction {@link com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade}
 * deliberately does not serve (it reads per connected user, not per catalog workflow). Gated the same way
 * {@code AutomationWorkflowProjectAdminFacade} already is: a class-level plain {@code isTenantAdmin()} guard rather
 * than a role literal, since the embedded admin console has no {@code ROLE_ADMIN} authority of its own -- and
 * class-level so a future method on this facade can't be added without the guard.
 *
 * <p>
 * The join back to {@link ConnectedUser} goes through {@link ConnectedUserProjectService} rather than a new method on
 * {@link ConnectedUserService} that would accept a {@link ConnectedUserProjectWorkflow}:
 * {@code embedded-configuration-api} already depends on {@code embedded-connected-user-api} (for
 * {@link ConnectedUserProject}), so the reverse dependency that a {@link ConnectedUserProjectWorkflow}-typed parameter
 * on {@link ConnectedUserService} would require is a circular module dependency. {@code getReferences} batches both
 * hops -- {@code ConnectedUserProjectService#getConnectedUserProjects(List)} then
 * {@code ConnectedUserService#getConnectedUsers(List)} -- into one query each, joining in memory via maps, instead of
 * issuing two per-row lookups (an unbounded 1+2N query pattern for large reference sets).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@PreAuthorize("isTenantAdmin()")
public class ConnectedUserCodeWorkflowReferenceAdminFacadeImpl
    implements ConnectedUserCodeWorkflowReferenceAdminFacade {

    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserCodeWorkflowReferenceAdminFacadeImpl(
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserService connectedUserService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserService = connectedUserService;
    }

    @Override
    public List<ConnectedUserCodeWorkflowReferenceDTO> getReferences(Set<String> catalogWorkflowUuids) {
        List<ConnectedUserProjectWorkflow> references =
            connectedUserProjectWorkflowRepository.findAllByCatalogWorkflowUuidIn(catalogWorkflowUuids);

        List<Long> connectedUserProjectIds = references.stream()
            .map(ConnectedUserProjectWorkflow::getConnectedUserProjectId)
            .distinct()
            .toList();

        Map<Long, ConnectedUserProject> connectedUserProjectMap =
            connectedUserProjectService.getConnectedUserProjects(connectedUserProjectIds)
                .stream()
                .collect(Collectors.toMap(ConnectedUserProject::getId, Function.identity()));

        List<Long> connectedUserIds = connectedUserProjectMap.values()
            .stream()
            .map(ConnectedUserProject::getConnectedUserId)
            .distinct()
            .toList();

        Map<Long, ConnectedUser> connectedUserMap = connectedUserService.getConnectedUsers(connectedUserIds)
            .stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));

        return references.stream()
            .map(reference -> toDTO(reference, connectedUserProjectMap, connectedUserMap))
            .toList();
    }

    private ConnectedUserCodeWorkflowReferenceDTO toDTO(
        ConnectedUserProjectWorkflow reference, Map<Long, ConnectedUserProject> connectedUserProjectMap,
        Map<Long, ConnectedUser> connectedUserMap) {

        ConnectedUserProject connectedUserProject = connectedUserProjectMap.get(
            reference.getConnectedUserProjectId());

        ConnectedUser connectedUser = connectedUserMap.get(connectedUserProject.getConnectedUserId());

        return new ConnectedUserCodeWorkflowReferenceDTO(
            reference.getCatalogWorkflowUuid(), connectedUser.getExternalId(), reference.isEnabled(),
            reference.isDangling(), reference.getDanglingReason());
    }
}
