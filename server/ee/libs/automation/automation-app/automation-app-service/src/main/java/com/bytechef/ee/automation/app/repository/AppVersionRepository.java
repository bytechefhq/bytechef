/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AppVersionRepository extends ListCrudRepository<AppVersion, Long> {

    List<AppVersion> findAllByAppIdOrderByIdDesc(long appId, Limit limit);

    Optional<AppVersion> findByAppIdAndName(long appId, String name);
}
