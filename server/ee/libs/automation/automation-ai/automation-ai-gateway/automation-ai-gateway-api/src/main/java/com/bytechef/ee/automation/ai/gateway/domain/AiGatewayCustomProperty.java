/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_custom_property")
public class AiGatewayCustomProperty {

    @Id
    private Long id;

    @Column
    private String key;

    @Column("request_log_id")
    private Long requestLogId;

    @Column("trace_id")
    private Long traceId;

    @Column
    private String value;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayCustomProperty() {
    }

    public AiGatewayCustomProperty(Long workspaceId, String key, String value) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(key, "key must not be blank");

        this.key = key;
        this.value = value;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayCustomProperty aiGatewayCustomProperty)) {
            return false;
        }

        return Objects.equals(id, aiGatewayCustomProperty.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public Long getRequestLogId() {
        return requestLogId;
    }

    public Long getTraceId() {
        return traceId;
    }

    public String getValue() {
        return value;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setRequestLogId(Long requestLogId) {
        this.requestLogId = requestLogId;
    }

    public void setTraceId(Long traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "AiGatewayCustomProperty{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", traceId=" + traceId +
            ", requestLogId=" + requestLogId +
            '}';
    }
}
