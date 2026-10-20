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

package com.bytechef.platform.mcp.web;

import com.bytechef.platform.constant.PlatformType;

/**
 * Describes the MCP servers of one {@link PlatformType}. The module that serves MCP servers of that type contributes
 * the bean, so the platform never hardcodes their paths or administration rules.
 *
 * @author Ivica Cardic
 */
public interface McpServerTypeDefinition {

    PlatformType getType();

    /**
     * @return whether the shared MCP server, component and tool operations may administer servers of this type; a type
     *         that is administered only through its own operations returns {@code false}
     */
    boolean isSharedAdministration();

    default boolean isSecretKeyReadableByEditors() {
        return false;
    }

    /**
     * @param secretKey the MCP server secret key, possibly masked
     * @return the path within the application, without the public URL
     */
    String resolvePath(String secretKey);
}
