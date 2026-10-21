/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.App;
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
public interface AppRepository extends ListCrudRepository<App, Long> {

    List<App> findAllByWorkspaceIdOrderByName(long workspaceId);

    Optional<App> findByUuid(String uuid);

    Optional<App> findByWorkspaceIdAndName(long workspaceId, String name);
}
