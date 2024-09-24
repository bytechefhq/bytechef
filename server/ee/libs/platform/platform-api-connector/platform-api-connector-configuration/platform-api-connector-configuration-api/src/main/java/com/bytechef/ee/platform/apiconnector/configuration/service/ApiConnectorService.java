/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorService {

    ApiConnector create(@NonNull ApiConnector apiConnector);

    void delete(long id);

    ApiConnector getApiConnector(long id);

    Optional<ApiConnector> fetchApiConnector(String componentName, int componentVersion);

    List<ApiConnector> getApiConnectors();

    ApiConnector update(@NonNull ApiConnector apiConnector);

    ApiConnector enableApiConnector(Long id, boolean enable);
}
