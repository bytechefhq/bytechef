/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.teardown;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Disables every enabled integration instance configuration of the tenant, which disables the triggers of its enabled
 * integration instances.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Order(200)
class IntegrationInstanceTenantTeardownContributor implements TenantTeardownContributor {

    private static final Logger log = LoggerFactory.getLogger(IntegrationInstanceTenantTeardownContributor.class);

    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @SuppressFBWarnings("EI")
    IntegrationInstanceTenantTeardownContributor(
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService) {

        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
    }

    @Override
    public List<String> onDeletionRequested(String tenantId) {
        List<String> failures = new ArrayList<>();

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurationService
            .getIntegrationInstanceConfigurations()) {

            if (!integrationInstanceConfiguration.isEnabled()) {
                continue;
            }

            long integrationInstanceConfigurationId = Objects.requireNonNull(integrationInstanceConfiguration.getId());

            try {
                integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationId, false);
            } catch (RuntimeException e) {
                log.error(
                    "Unable to disable integration instance configuration {} of tenant {}",
                    integrationInstanceConfigurationId, tenantId, e);

                failures.add(
                    "integrationInstanceConfiguration " + integrationInstanceConfigurationId + ": " + e.getMessage());
            }
        }

        return failures;
    }
}
