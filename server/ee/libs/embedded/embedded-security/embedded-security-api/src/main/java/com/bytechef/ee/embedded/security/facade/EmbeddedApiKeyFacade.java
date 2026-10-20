/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.facade;

import com.bytechef.platform.security.domain.ApiKey;
import java.util.List;

/**
 * API keys of the embedded side. They are shared per environment: anyone holding the embedded API key scope in that
 * environment manages them, regardless of who created them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedApiKeyFacade {

    String createApiKey(ApiKey apiKey);

    void deleteApiKey(long id);

    ApiKey getApiKey(long id);

    List<ApiKey> getApiKeys(long environmentId);

    ApiKey updateApiKey(ApiKey apiKey);
}
