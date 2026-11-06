/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.dto;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ExternalInvocationLogDTO(ExternalInvocationLog externalInvocationLog, @Nullable String workflowName) {
}
