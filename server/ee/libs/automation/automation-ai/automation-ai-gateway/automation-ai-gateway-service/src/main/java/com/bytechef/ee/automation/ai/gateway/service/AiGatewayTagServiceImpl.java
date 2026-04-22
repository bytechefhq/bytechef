/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayTagRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiGatewayTagServiceImpl implements AiGatewayTagService {

    private final AiGatewayTagRepository repository;

    AiGatewayTagServiceImpl(AiGatewayTagRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AiGatewayTag> findByWorkspaceIdAndName(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        return repository.findByWorkspaceIdAndName(workspaceId, name);
    }

    @Override
    public AiGatewayTag create(AiGatewayTag tag) {
        Validate.notNull(tag, "tag must not be null");
        Validate.isTrue(tag.getId() == null, "tag id must be null for creation");

        return repository.save(tag);
    }

    @Override
    public void delete(long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayTag getTag(long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiGatewayTag not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayTag> getTagsByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return repository.findAllByWorkspaceIdOrderByNameAsc(workspaceId);
    }

    @Override
    public AiGatewayTag update(long id, String name, String color) {
        AiGatewayTag tag = getTag(id);

        if (name != null) {
            tag.setName(name);
        }

        tag.setColor(color);

        return repository.save(tag);
    }
}
