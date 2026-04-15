/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.resource.grant.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Reads and writes the named-user grants that let someone see a resource its owner has withheld.
 *
 * <p>
 * This service does no authorization of its own: callers are responsible for checking that the actor may manage the
 * resource's grants and that the grantee belongs to the resource's workspace. Keeping those checks at the facade means
 * one policy governs every entry point rather than being split between a facade and a service.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ResourceGrantService {

    /**
     * Grants {@code userId} visibility of the resource. Idempotent: re-granting an existing triple succeeds without
     * creating a second row, so a double-click cannot produce duplicates or an error.
     */
    void grant(String resourceType, long resourceId, long userId);

    /**
     * Revokes the grant. Silent when no grant exists — the caller asked for the row to be gone and it is.
     */
    void revoke(String resourceType, long resourceId, long userId);

    /**
     * The users currently granted the resource.
     */
    List<Long> getGrantedUserIds(String resourceType, long resourceId);

    /**
     * The subset of {@code resourceIds} that {@code userId} has been granted. One query for the whole candidate set
     * rather than one per resource; returns empty without touching the database when the candidate set is empty.
     */
    Set<Long> filterGrantedResourceIds(String resourceType, long userId, Collection<Long> resourceIds);

    /**
     * Removes every grant for a resource. Called when the resource itself is deleted, so grants cannot outlive their
     * target and later collide with a recycled id.
     */
    void deleteGrants(String resourceType, long resourceId);
}
