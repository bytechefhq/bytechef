/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppConnection;
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
public interface AppConnectionRepository extends ListCrudRepository<AppConnection, Long> {

    List<AppConnection> findAllByAppIdOrderByBindingKey(long appId);

    List<AppConnection> findAllByConnectionId(long connectionId);

    Optional<AppConnection> findByAppIdAndBindingKey(long appId, String bindingKey);
}
