/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionContributor;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lists the organization connections in every workspace and deletes a connection's grants with it, since
 * {@code resource_grant.resource_id} is polymorphic and has no foreign key to cascade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class WorkspaceConnectionGrantContributor implements WorkspaceConnectionContributor {

    private static final String CONNECTION = "Connection";

    private final ConnectionService connectionService;
    private final ResourceGrantService resourceGrantService;

    WorkspaceConnectionGrantContributor(
        ConnectionService connectionService, ResourceGrantService resourceGrantService) {

        this.connectionService = connectionService;
        this.resourceGrantService = resourceGrantService;
    }

    @Override
    public List<Long> getSharedConnectionIds() {
        return CollectionUtils.map(
            connectionService.getConnectionsByVisibility(ResourceVisibility.ORGANIZATION, PlatformType.AUTOMATION),
            Connection::getId);
    }

    @Override
    public void beforeDelete(long connectionId) {
        resourceGrantService.deleteGrants(CONNECTION, connectionId);
    }
}
