/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.repository;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ApiClientRepository extends ListCrudRepository<ApiClient, Long> {

    Optional<ApiClient> findBySecretKeyAndEnvironment(String secretKey, int environment);
}
