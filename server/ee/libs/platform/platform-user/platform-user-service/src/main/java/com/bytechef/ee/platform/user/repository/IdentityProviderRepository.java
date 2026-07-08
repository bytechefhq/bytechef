/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.repository;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
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

    @Query("SELECT ip.* FROM identity_provider ip " +
        "WHERE ip.enabled = true AND ip.mcp_embedded = true LIMIT 1")
    Optional<IdentityProvider> findMcpIdentityProvider();
}
