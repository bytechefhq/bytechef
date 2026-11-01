/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.platform.configuration.domain.Environment;
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
public interface ToolInvocationFacade {

    Page<ToolInvocationLog> getToolInvocations(
        String externalUserId, Environment environment, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber);
}
