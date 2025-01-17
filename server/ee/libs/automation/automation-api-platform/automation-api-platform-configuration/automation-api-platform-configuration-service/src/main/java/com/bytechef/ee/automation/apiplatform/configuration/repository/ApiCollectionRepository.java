/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.repository;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ApiCollectionRepository
    extends ListPagingAndSortingRepository<ApiCollection, Long>, ListCrudRepository<ApiCollection, Long>,
    CustomApiCollectionRepository {

    @Query("""
        SELECT project_deployment.project_id FROM project_deployment WHERE project_deployment.id IN
        (SELECT api_collection.project_deployment_id FROM api_collection)""")
    List<Long> findAllApiCollectionProjectIds();
}
