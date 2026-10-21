/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppApiCollection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AppApiCollectionRepository extends ListCrudRepository<AppApiCollection, Long> {

    List<AppApiCollection> findAllByApiCollectionId(long apiCollectionId);

    List<AppApiCollection> findAllByAppIdOrderByApiCollectionId(long appId);

    Optional<AppApiCollection> findByAppIdAndApiCollectionId(long appId, long apiCollectionId);
}
