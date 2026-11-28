/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval.repository;

import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
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
public interface AiHubToolApprovalRepository extends CrudRepository<AiHubToolApproval, Long> {

    List<AiHubToolApproval> findAllByChatIdOrderByCreatedDateDesc(long chatId);

    Optional<AiHubToolApproval> findFirstByChatIdAndStatus(long chatId, int status);

    List<AiHubToolApproval> findAllByChatIdAndStatus(long chatId, int status);

    void deleteAllByChatId(long chatId);
}
