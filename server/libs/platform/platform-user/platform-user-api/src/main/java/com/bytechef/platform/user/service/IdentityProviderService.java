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

package com.bytechef.platform.user.service;

import com.bytechef.platform.user.domain.IdentityProvider;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface IdentityProviderService {

    IdentityProvider create(IdentityProvider identityProvider);

    void delete(long id);

    Optional<IdentityProvider> fetchByDomain(String emailDomain);

    Optional<IdentityProvider> fetchByName(String name);

    Optional<IdentityProvider> fetchByScimApiKey(String scimApiKey);

    IdentityProvider getIdentityProvider(long id);

    List<IdentityProvider> getIdentityProviders();

    String getDecryptedClientSecret(IdentityProvider identityProvider);

    IdentityProvider update(IdentityProvider identityProvider);
}
