/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiClientFacade {

    String createApiClient(ApiClient apiClient);

    void deleteApiClient(long id);

    ApiClient getApiClient(long id);

    List<ApiClient> getApiClients();

    ApiClient updateApiClient(ApiClient apiClient);
}
