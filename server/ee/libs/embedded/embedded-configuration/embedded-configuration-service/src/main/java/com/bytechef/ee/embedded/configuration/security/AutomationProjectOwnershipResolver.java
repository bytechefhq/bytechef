/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectRepository;
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
public class AutomationProjectOwnershipResolver implements ResourceOwnershipResolver {

    private final AutomationProjectRepository automationProjectRepository;

    @SuppressFBWarnings("EI")
    public AutomationProjectOwnershipResolver(
        AutomationProjectRepository automationProjectRepository) {

        this.automationProjectRepository = automationProjectRepository;
    }

    @Override
    public String resourceType() {
        return "AutomationProject";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return automationProjectRepository.findByProjectId(id)
            .isPresent() ? ResourceOwner.ofEmbedded() : ResourceOwner.unknown();
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}
