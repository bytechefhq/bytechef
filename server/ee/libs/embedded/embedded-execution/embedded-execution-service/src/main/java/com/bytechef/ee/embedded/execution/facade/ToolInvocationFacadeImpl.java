/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
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
    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationFacadeImpl(
        ConnectedUserService connectedUserService, ToolInvocationLogService toolInvocationLogService) {

        this.connectedUserService = connectedUserService;
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public Page<ToolInvocationLog> getToolInvocations(
        String externalUserId, Environment environment, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return toolInvocationLogService.getToolInvocationLogs(
            PlatformType.EMBEDDED, environment.ordinal(), surface, outcome, null, connectedUser.getId(), null,
            startDate, endDate, pageNumber);
    }
}
