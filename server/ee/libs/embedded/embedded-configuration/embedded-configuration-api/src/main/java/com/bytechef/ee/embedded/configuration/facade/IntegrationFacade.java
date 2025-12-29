/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    long createIntegration(IntegrationDTO integrationDTO);

    void deleteIntegration(long id);

    IntegrationDTO getIntegration(long id);

    List<IntegrationDTO> getIntegrations(
        @Nullable Long categoryId, boolean integrationInstanceConfigurations, @Nullable Long tagId,
        @Nullable Status status, boolean includeAllFields);

    void publishIntegration(long id, @Nullable String description);

    void updateIntegration(IntegrationDTO integration);
}
