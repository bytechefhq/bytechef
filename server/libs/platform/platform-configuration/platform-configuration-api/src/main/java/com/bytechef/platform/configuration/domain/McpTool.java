/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing an MCP tool.
 *
 * @author Ivica Cardic
 */
@Table("mcp_tool")
public final class McpTool {

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private MapWrapper parameters;

    @Column("mcp_component_id")
    private AggregateReference<McpComponent, Long> mcpComponentId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public McpTool() {
    }

    public McpTool(String name, Map<String, String> parameters, Long mcpComponentId) {
        this.name = name;
        this.parameters = new MapWrapper(parameters);
        this.mcpComponentId = mcpComponentId == null ? null : AggregateReference.to(mcpComponentId);
    }

    public McpTool(Long id, String name, Map<String, String> parameters, Long mcpComponentId) {
        this.id = id;
        this.name = name;
        this.parameters = new MapWrapper(parameters);
        this.mcpComponentId = AggregateReference.to(mcpComponentId);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public Map<String, ?> getParameters() {
        return parameters.getMap();
    }

    public Long getMcpComponentId() {
        return mcpComponentId.getId();
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        McpTool mcpTool = (McpTool) o;

        return Objects.equals(id, mcpTool.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = new MapWrapper(parameters);
    }

    public void setMcpComponentId(Long mcpComponentId) {
        this.mcpComponentId = AggregateReference.to(mcpComponentId);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "McpTool{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", parameters='" + parameters + '\'' +
            ", mcpComponentId=" + mcpComponentId +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
