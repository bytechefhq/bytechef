/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.repository;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ConnectedUserRepository
    extends ListPagingAndSortingRepository<ConnectedUser, Long>, ListCrudRepository<ConnectedUser, Long>,
    CustomConnectedUserRepository {

    int DEFAULT_PAGE_SIZE = 20;

    Optional<ConnectedUser> findByExternalIdAndEnvironment(String externalId, int environment);
}
