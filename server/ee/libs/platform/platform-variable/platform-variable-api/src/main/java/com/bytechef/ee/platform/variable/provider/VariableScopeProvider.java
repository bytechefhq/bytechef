/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.provider;

import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;

/**
 * Per-{@link PlatformType} SPI that maps a job principal or a workflow id onto the {@link VariableScope} whose
 * variables it should see. Contributed by the automation and embedded configuration modules so that
 * {@code platform-variable} depends on neither.
 *
 * @version ee
 */
public interface VariableScopeProvider {

    PlatformType getType();

    /**
     * Scope for a job created for {@code jobPrincipalId} (project deployment id / integration instance id).
     */
    Optional<VariableScope> getVariableScope(long jobPrincipalId);

    /**
     * Scope for editor previews / test runs of {@code workflowId}; empty when this provider does not own it.
     */
    Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId);
}
