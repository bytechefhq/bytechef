/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiGatewayTagService {

    AiGatewayTag create(AiGatewayTag tag);

    void delete(long id);

    Optional<AiGatewayTag> findByWorkspaceIdAndName(Long workspaceId, String name);

    AiGatewayTag getTag(long id);

    List<AiGatewayTag> getTagsByWorkspace(Long workspaceId);

    AiGatewayTag update(long id, String name, String color);
}
