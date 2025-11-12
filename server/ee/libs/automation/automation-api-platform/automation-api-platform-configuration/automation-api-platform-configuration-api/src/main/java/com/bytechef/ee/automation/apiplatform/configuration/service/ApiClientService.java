/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiClientService {

    String create(ApiClient apiKey);

    void delete(long id);

    Optional<ApiClient> fetchApiClient(String secretKey, long environmentId);

    ApiClient getApiClient(long id);

    List<ApiClient> getApiClients();

    ApiClient update(ApiClient apiClient);
}
