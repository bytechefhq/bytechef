/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteIntegrationFacadeClient implements IntegrationFacade {

    @Override
    public long createIntegration(IntegrationDTO integrationDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIntegration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationDTO getIntegration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstanceConfigurations, Long tagId, Status status,
        boolean includeAllFields) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void publishIntegration(long id, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateIntegration(IntegrationDTO integration) {
        throw new UnsupportedOperationException();
    }
}
