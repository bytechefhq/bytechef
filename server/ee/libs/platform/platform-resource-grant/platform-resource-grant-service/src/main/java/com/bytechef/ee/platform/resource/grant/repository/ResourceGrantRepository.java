/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.resource.grant.repository;

import com.bytechef.ee.platform.resource.grant.domain.ResourceGrant;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ResourceGrantRepository extends ListCrudRepository<ResourceGrant, Long> {

    List<ResourceGrant> findAllByResourceTypeAndResourceId(String resourceType, long resourceId);

    Optional<ResourceGrant> findByResourceTypeAndResourceIdAndUserId(
        String resourceType, long resourceId, long userId);

    /**
     * Projects to ids only: the caller wants set membership, and materializing whole aggregates to read one column each
     * would be wasted work on the hot list path.
     */
    @Query("""
        SELECT resource_id FROM resource_grant
         WHERE resource_type = :resourceType
           AND user_id = :userId
           AND resource_id IN (:resourceIds)
        """)
    List<Long> findGrantedResourceIds(
        @Param("resourceType") String resourceType, @Param("userId") long userId,
        @Param("resourceIds") Collection<Long> resourceIds);

    /**
     * Insert-if-absent, made idempotent by the database rather than by catching a duplicate-key exception.
     *
     * <p>
     * The exception route does not work here: PostgreSQL aborts the transaction on a constraint violation and Spring
     * marks it rollback-only, so catching {@code DuplicateKeyException} still fails at commit with
     * {@code UnexpectedRollbackException}. {@code ON CONFLICT DO NOTHING} means the conflict never becomes an error,
     * which also closes the race that a read-then-write check would leave open.
     *
     * <p>
     * Audit columns are supplied explicitly because this bypasses the entity save path that {@code @CreatedBy} and
     * {@code @CreatedDate} hook into.
     */
    @Modifying
    @Query("""
        INSERT INTO resource_grant (resource_type, resource_id, user_id, created_by, created_date)
             VALUES (:resourceType, :resourceId, :userId, :createdBy, :createdDate)
        ON CONFLICT (resource_type, resource_id, user_id) DO NOTHING
        """)
    void insertIfAbsent(
        @Param("resourceType") String resourceType, @Param("resourceId") long resourceId,
        @Param("userId") long userId, @Param("createdBy") String createdBy,
        @Param("createdDate") Instant createdDate);

    void deleteAllByResourceTypeAndResourceId(String resourceType, long resourceId);
}
