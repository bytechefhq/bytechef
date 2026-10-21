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

import com.bytechef.platform.oauth2.authorizationserver.domain.ClientRegistrationRequest;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import java.util.List;
import java.util.Optional;

/**
 * Lists, registers, updates and deletes registered OAuth2 clients.
 *
 * @author Ivica Cardic
 */
public interface RegisteredClientService {

    /**
     * Deletes a registered client with its authorizations and consents.
     *
     * @throws com.bytechef.exception.ConfigurationException with {@code REGISTERED_CLIENT_MANAGED_BY_SERVER} for a
     *                                                       client the server registered for an app instance
     * @return whether a registered client was deleted
     */
    boolean deleteRegisteredClient(String id);

    /**
     * Deletes a client registered by {@link #registerClient(ClientRegistrationRequest)}, after its authorizations and
     * consents in the current tenant, whether or not multi-tenancy is enabled.
     *
     * @throws com.bytechef.exception.ConfigurationException when the client was registered some other way
     * @return whether a registered client was deleted
     */
    boolean deleteClient(String clientId);

    /**
     * Returns the id of the registered client with the given OAuth2 client id.
     */
    Optional<String> fetchRegisteredClientId(String clientId);

    /**
     * Returns the details the consent page shows about the registered client with the given OAuth2 client id.
     */
    Optional<OAuth2ConsentClient> fetchConsentClient(String clientId);

    List<RegisteredClientInfo> getRegisteredClients(boolean deletable);

    /**
     * Registers a public authorization code client that must use PKCE.
     *
     * @throws com.bytechef.exception.ConfigurationException when the client id is taken, a redirect URI is not an
     *                                                       absolute https or loopback http URI, or the scopes mix the
     *                                                       app scope with an MCP scope
     * @return the id of the registered client
     */
    String registerClient(ClientRegistrationRequest clientRegistrationRequest);

    /**
     * Replaces the name, redirect URIs and consent setting of a client registered by
     * {@link #registerClient(ClientRegistrationRequest)}. The scopes of the request must equal the registered ones.
     *
     * @throws com.bytechef.exception.ConfigurationException when no such client exists, it was registered some other
     *                                                       way, the scopes differ from the registered ones, or the
     *                                                       request fails the registration rules
     */
    void updateClient(ClientRegistrationRequest clientRegistrationRequest);
}
