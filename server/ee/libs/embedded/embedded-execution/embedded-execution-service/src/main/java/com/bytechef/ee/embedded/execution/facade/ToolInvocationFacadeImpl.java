/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ToolInvocationFacadeImpl implements ToolInvocationFacade {

    private final ConnectedUserService connectedUserService;
    private final ExternalInvocationLogService externalInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationFacadeImpl(
        ConnectedUserService connectedUserService, ExternalInvocationLogService externalInvocationLogService) {

        this.connectedUserService = connectedUserService;
        this.externalInvocationLogService = externalInvocationLogService;
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public Page<ExternalInvocationLog> getToolInvocations(
        String externalUserId, Environment environment, @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.EMBEDDED, environment.ordinal(), surface, outcome, null, null, connectedUser.getId(), null,
            startDate, endDate, pageNumber);
    }
}
