/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@PreAuthorize("isTenantAdmin()")
public class IntegrationInstanceAdminFacadeImpl implements IntegrationInstanceAdminFacade {

    private final IntegrationInstanceFacade integrationInstanceFacade;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceAdminFacadeImpl(IntegrationInstanceFacade integrationInstanceFacade) {
        this.integrationInstanceFacade = integrationInstanceFacade;
    }

    @Override
    public void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable) {
        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceId, workflowId, enable);
    }
}
