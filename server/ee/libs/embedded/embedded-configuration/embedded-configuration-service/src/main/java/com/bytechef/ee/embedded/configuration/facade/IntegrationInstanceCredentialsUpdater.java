/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.audit.IntegrationAuditEvents;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class IntegrationInstanceCredentialsUpdater {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @SuppressFBWarnings("EI")
    IntegrationInstanceCredentialsUpdater(ConnectedUserConnectionFacade connectedUserConnectionFacade) {
        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
    }

    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CREDENTIALS_UPDATED, data = {
        @AuditData(key = "integrationInstanceId", value = "#integrationInstanceId"),
        @AuditData(key = "connectionId", value = "#connectionId"),
        @AuditData(key = "connectedUserId", value = "#connectedUserId")
    })
    @PreAuthorize("isTenantAdmin()")
    void updateCredentials(
        long integrationInstanceId, long connectedUserId, long connectionId, Map<String, ?> parameters,
        int version) {

        connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
            connectedUserId, connectionId, parameters, version);
    }
}
