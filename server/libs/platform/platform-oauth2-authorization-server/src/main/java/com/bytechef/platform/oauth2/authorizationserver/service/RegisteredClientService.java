/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.oauth2.authorizationserver.service;

import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import java.util.List;
import java.util.Optional;

/**
 * Lists and deletes registered OAuth2 clients.
 *
 * @author Ivica Cardic
 */
public interface RegisteredClientService {

    /**
     * Deletes a registered client with its authorizations and consents.
     *
     * @throws com.bytechef.exception.ConfigurationException when multi-tenancy is enabled, because registered clients
     *                                                       are shared by every tenant
     */
    void deleteRegisteredClient(String id);

    /**
     * Returns the details the consent page shows about the registered client with the given OAuth2 client id.
     */
    Optional<OAuth2ConsentClient> fetchConsentClient(String clientId);

    List<RegisteredClientInfo> getRegisteredClients();
}
