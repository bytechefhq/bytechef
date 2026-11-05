/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval.repository;

import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRule;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubToolApprovalRuleRepository extends CrudRepository<AiHubToolApprovalRule, Long> {

    List<AiHubToolApprovalRule> findAllByWorkspaceId(long workspaceId);
}
