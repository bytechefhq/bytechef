/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import java.time.LocalDate;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserFacade {

    void enableConnectedUser(long id, boolean enable);

    ConnectedUserDTO getConnectedUser(long id);

    /**
     * Tenant-admin-gated passthrough returning the raw {@link ConnectedUser} entity for the management GraphQL surface
     * (which maps the entity directly, unlike the enriched {@link ConnectedUserDTO}). Lives on the facade so the gate
     * is not on the controller and the runtime-shared {@code ConnectedUserService.getConnectedUser(long)} stays
     * ungated.
     */
    ConnectedUser getConnectedUserEntity(long id);

    Page<ConnectedUserDTO> getConnectedUsers(
        Long environmentId, String search, CredentialStatus credentialStatus, LocalDate createDateFrom,
        LocalDate createDateTo, Long integrationId, int pageNumber);

    /**
     * Tenant-admin-gated passthrough returning a page of raw {@link ConnectedUser} entities for the management GraphQL
     * surface.
     */
    Page<ConnectedUser> getConnectedUserEntities(
        Long environmentId, String name, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        int pageNumber);
}
