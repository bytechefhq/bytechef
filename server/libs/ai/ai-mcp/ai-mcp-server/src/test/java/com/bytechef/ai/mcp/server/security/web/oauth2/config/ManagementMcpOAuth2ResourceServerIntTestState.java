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

package com.bytechef.ai.mcp.server.security.web.oauth2.config;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.user.domain.User;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpOAuth2ResourceServerIntTestState {

    private final AtomicReference<Optional<User>> adminUser = new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<Property>> mcpServerProperty = new AtomicReference<>(Optional.empty());

    public Optional<User> getAdminUser() {
        return adminUser.get();
    }

    public Optional<Property> getMcpServerProperty() {
        return mcpServerProperty.get();
    }

    public void setAdminUser(Optional<User> user) {
        adminUser.set(user);
    }

    public void setMcpServerProperty(Optional<Property> property) {
        mcpServerProperty.set(property);
    }
}
