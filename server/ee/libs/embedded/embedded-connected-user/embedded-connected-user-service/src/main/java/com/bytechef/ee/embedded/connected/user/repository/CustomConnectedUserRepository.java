/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.repository;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface CustomConnectedUserRepository {

    Page<ConnectedUser> findAll(
        Integer environment, String search, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        Pageable pageable);
}
