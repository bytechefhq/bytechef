/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AutomationWorkflowProjectOwnershipResolver implements ResourceOwnershipResolver {

    private final AutomationWorkflowProjectRepository automationWorkflowProjectRepository;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectOwnershipResolver(
        AutomationWorkflowProjectRepository automationWorkflowProjectRepository) {

        this.automationWorkflowProjectRepository = automationWorkflowProjectRepository;
    }

    @Override
    public String resourceType() {
        return "AutomationWorkflowProject";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return automationWorkflowProjectRepository.findByProjectId(id)
            .isPresent() ? ResourceOwner.ofEmbedded() : ResourceOwner.unknown();
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}
