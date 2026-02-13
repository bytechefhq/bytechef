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

package com.bytechef.platform.user.repository;

import com.bytechef.platform.user.domain.IdentityProvider;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IdentityProviderRepository extends ListCrudRepository<IdentityProvider, Long> {

    @Query("SELECT ip.* FROM identity_provider ip " +
        "JOIN identity_provider_domain ipd ON ip.id = ipd.identity_provider_id " +
        "WHERE ipd.domain = :domain AND ip.enabled = true")
    Optional<IdentityProvider> findByDomain(@Param("domain") String domain);

    @Query("SELECT ip.* FROM identity_provider ip " +
        "WHERE LOWER(ip.name) = LOWER(:name) AND ip.enabled = true")
    Optional<IdentityProvider> findByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT ip.* FROM identity_provider ip " +
        "WHERE ip.scim_api_key = :scimApiKey")
    Optional<IdentityProvider> findByScimApiKey(@Param("scimApiKey") String scimApiKey);
}
