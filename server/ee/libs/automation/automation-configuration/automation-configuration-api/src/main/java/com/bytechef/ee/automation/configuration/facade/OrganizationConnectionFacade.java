/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;

/**
 * Facade for organization-scoped connections — connections visible to every member across every workspace.
 *
 * <p>
 * <b>EE-only.</b> The bean is gated with {@code @ConditionalOnEEVersion}; it is never registered on CE. Authorization
 * is enforced on the facade implementation with {@code @PreAuthorize(ROLE_ADMIN)} — non-admin users receive 403.
 *
 * <p>
 * <b>Terminal visibility.</b> {@code create} always persists with
 * {@link com.bytechef.platform.connection.domain.ResourceVisibility#ORGANIZATION}. Admins cannot demote an organization
 * connection to a narrower visibility — they must delete and recreate. This keeps organization-level credentials from
 * silently being narrowed to workspace-only by a routine share-list edit.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface OrganizationConnectionFacade {

    long create(ConnectionDTO connectionDTO);

    void delete(long connectionId);

    List<ConnectionDTO> getOrganizationConnections(Long environmentId);

    ConnectionDTO update(long connectionId, String name, List<Long> tagIds, int version);
}
