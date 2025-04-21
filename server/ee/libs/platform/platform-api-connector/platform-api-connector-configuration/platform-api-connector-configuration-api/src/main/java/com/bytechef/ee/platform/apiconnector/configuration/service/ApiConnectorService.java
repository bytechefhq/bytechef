/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorService {

    ApiConnector create(ApiConnector apiConnector);

    void delete(long id);

    ApiConnector getApiConnector(long id);

    Optional<ApiConnector> fetchApiConnector(String componentName, int componentVersion);

    List<ApiConnector> getApiConnectors();

    ApiConnector update(ApiConnector apiConnector);

    ApiConnector enableApiConnector(long id, boolean enable);
}
