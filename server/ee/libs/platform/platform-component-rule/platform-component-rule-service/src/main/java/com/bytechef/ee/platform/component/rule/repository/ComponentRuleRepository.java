/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.repository;

import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface ComponentRuleRepository extends CrudRepository<ComponentRule, Long> {

    List<ComponentRule> findAllByComponentName(String componentName);

    @Query("""
        SELECT * FROM component_rule
        WHERE component_name = :componentName
          AND enabled = TRUE
          AND (workspace_id IS NULL OR workspace_id = :workspaceId)
        """)
    List<ComponentRule> findAllEnabledForWorkspace(
        @Param("componentName") String componentName, @Param("workspaceId") @Nullable Long workspaceId);
}
