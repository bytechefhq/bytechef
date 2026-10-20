/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedConnectionOwnershipResolver implements ResourceOwnershipResolver {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public EmbeddedConnectionOwnershipResolver(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return connectionService.fetchConnection(id)
            .map(Connection::getType)
            .filter(type -> type == PlatformType.EMBEDDED)
            .map(type -> ResourceOwner.ofEmbedded())
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}
