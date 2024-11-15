/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiClientService {

    String create(@NonNull ApiClient apiKey);

    void delete(long id);

    Optional<ApiClient> fetchApiClient(@NonNull String secretKey);

    ApiClient getApiClient(long id);

    List<ApiClient> getApiClients();

    ApiClient update(@NonNull ApiClient apiClient);
}
