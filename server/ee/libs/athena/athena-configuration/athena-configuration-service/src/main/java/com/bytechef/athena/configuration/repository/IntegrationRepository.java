/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.repository;

import com.bytechef.athena.configuration.domain.Integration;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationRepository
    extends ListPagingAndSortingRepository<Integration, Long>, ListCrudRepository<Integration, Long> {

    List<Integration> findAllByCategoryIdOrderByName(long categoryId);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration_tag.tag_id = :tagId
            ORDER BY name
        """)
    List<Integration> findAllByTagIdOrderByName(@Param("tagId") long tagId);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration.category_id = :categoryId
            AND integration_tag.tag_id = :tagId
            ORDER BY name
        """)
    List<Integration> findAllByCategoryIdAndTagIdOrderByName(
        @Param("categoryId") long categoryId, @Param("tagId") long tagId);
}
