/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppDeployment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AppDeploymentRepository extends ListCrudRepository<AppDeployment, Long> {

    boolean existsByAppInstanceIdAndAppVersionIdAndStatus(long appInstanceId, long appVersionId, int status);

    boolean existsByAppInstanceIdAndStatusIn(long appInstanceId, Collection<Integer> statuses);

    List<AppDeployment> findAllByAppInstanceIdOrderByIdDesc(long appInstanceId, Limit limit);

    List<AppDeployment> findAllByStatusInOrderById(Collection<Integer> statuses);
}
