/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedJobOwnershipResolver implements ResourceOwnershipResolver {

    private final PrincipalJobService principalJobService;

    @SuppressFBWarnings("EI")
    public EmbeddedJobOwnershipResolver(PrincipalJobService principalJobService) {
        this.principalJobService = principalJobService;
    }

    @Override
    public String resourceType() {
        return "Job";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return principalJobService.fetchJobPrincipalId(id, PlatformType.EMBEDDED)
            .map(principalId -> ResourceOwner.ofEmbedded())
            .orElseGet(ResourceOwner::unknown);
    }
}
