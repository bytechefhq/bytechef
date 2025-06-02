/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserIntegrationFacade {

    ConnectedUserIntegrationDTO getConnectedUserIntegration(
        long integrationId, boolean enabled, Environment environment);

    List<ConnectedUserIntegrationDTO> getConnectedUserIntegrations(boolean enabled, Environment environment);
}
