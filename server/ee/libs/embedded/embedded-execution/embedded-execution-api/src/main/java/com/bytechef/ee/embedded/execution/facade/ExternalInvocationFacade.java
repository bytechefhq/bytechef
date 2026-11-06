/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ExternalInvocationFacade {

    Page<ExternalInvocationLog> getExternalInvocations(
        String externalUserId, Environment environment, @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber);
}
