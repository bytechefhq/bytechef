/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.repository;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ApiEndpointRepository extends ListCrudRepository<ApiCollectionEndpoint, Long> {

    List<ApiCollectionEndpoint> findByApiCollectionIdOrderByHttpMethod(long apiCollectionId);
}
