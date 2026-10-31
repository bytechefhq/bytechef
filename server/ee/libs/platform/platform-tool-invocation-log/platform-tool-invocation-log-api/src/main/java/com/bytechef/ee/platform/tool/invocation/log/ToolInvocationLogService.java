/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolInvocationLogService {

    ToolInvocationLog create(ToolInvocationLog toolInvocationLog);

    int deleteToolInvocationLogsCreatedBefore(Instant instant);

    Page<ToolInvocationLog> getToolInvocationLogs(
        @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber);
}
