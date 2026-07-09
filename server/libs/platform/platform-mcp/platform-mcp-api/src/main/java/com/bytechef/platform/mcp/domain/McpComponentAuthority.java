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

package com.bytechef.platform.mcp.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An authority that grants access to an {@link McpComponent}'s tools when the component's server enforces tool
 * authorization. A principal may use the component if it holds at least one of the component's authorities.
 *
 * @author Ivica Cardic
 */
@Table("mcp_component_authority")
public final class McpComponentAuthority {

    @Column
    private String authority;

    private McpComponentAuthority() {
    }

    public McpComponentAuthority(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        McpComponentAuthority that = (McpComponentAuthority) o;

        return Objects.equals(authority, that.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority);
    }

    @Override
    public String toString() {
        return "McpComponentAuthority{" +
            "authority='" + authority + '\'' +
            '}';
    }
}
