/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.repository;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface ComponentOperationPolicyRepository extends CrudRepository<ComponentOperationPolicy, Long> {

    List<ComponentOperationPolicy> findAllByComponentName(String componentName);

    Optional<ComponentOperationPolicy> findByComponentNameAndOperationTypeAndOperationName(
        String componentName, int operationType, String operationName);
}
