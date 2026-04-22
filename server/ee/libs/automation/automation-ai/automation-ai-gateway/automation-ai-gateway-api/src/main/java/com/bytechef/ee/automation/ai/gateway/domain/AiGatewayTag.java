/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Workspace-scoped tag used across AI Gateway resources (routing policies, traces, prompts). Distinct from the platform
 * {@code com.bytechef.platform.tag.domain.Tag} because the spec calls for workspace-scoped definitions with optional
 * color metadata; platform tags are global.
 *
 * @version ee
 */
@Table("ai_gateway_tag")
public class AiGatewayTag {

    @Column
    private String color;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayTag() {
    }

    public AiGatewayTag(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        this.workspaceId = workspaceId;
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayTag tag)) {
            return false;
        }

        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getColor() {
        return color;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }
}
