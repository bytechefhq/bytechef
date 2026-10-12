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

import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import java.util.List;

/**
 * Lists and revokes the per-user grants of a registered OAuth2 client in the current tenant.
 *
 * @author Ivica Cardic
 */
public interface OAuth2AuthorizationGrantService {

    List<OAuth2AuthorizationGrant> getGrants(String registeredClientId);

    /**
     * Deletes the authorizations and the consent one principal granted to a registered client.
     *
     * @throws com.bytechef.exception.ConfigurationException when the principal has nothing to revoke
     */
    void revokeGrant(String registeredClientId, String principalName);

    /**
     * Deletes the authorizations and consents every principal of the current tenant granted to a registered client.
     *
     * @throws com.bytechef.exception.ConfigurationException when there is nothing to revoke
     */
    void revokeGrants(String registeredClientId);
}
