/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorFacade {

    ApiConnector importOpenApiSpecification(String componentName, String specification);

    List<ApiConnectorDTO> getApiConnectors();
}
