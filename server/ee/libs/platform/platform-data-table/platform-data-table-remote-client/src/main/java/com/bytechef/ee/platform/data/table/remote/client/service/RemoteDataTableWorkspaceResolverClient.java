/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteDataTableWorkspaceResolverClient implements DataTableWorkspaceResolver {

    @Override
    public OptionalLong resolveByWorkflowId(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OptionalLong resolveByJobPrincipalId(long jobPrincipalId, PlatformType platformType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OptionalLong resolveByWorkflowUuid(String workflowUuid) {
        throw new UnsupportedOperationException();
    }
}
