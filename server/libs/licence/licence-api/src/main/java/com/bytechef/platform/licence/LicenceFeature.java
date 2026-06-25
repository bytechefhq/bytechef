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

package com.bytechef.platform.licence;

import java.util.Optional;

public enum LicenceFeature {

    SSO("sso"),
    AUDIT_LOG("audit-log"),
    CUSTOM_COMPONENTS("custom-components"),
    COMPONENT_POLICIES("component-policies"),
    API_CONNECTORS("api-connectors"),
    AI_PROVIDERS("ai-providers"),
    AI_COPILOT("ai-copilot"),
    GIT_SYNC("git-sync"),
    ADMIN_API_KEYS("admin-api-keys"),
    CONNECTION_VISIBILITY("connection-visibility"),
    MCP_SERVER("mcp-server");

    private final String key;

    LicenceFeature(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Optional<LicenceFeature> ofKey(String key) {
        for (LicenceFeature feature : values()) {
            if (feature.key.equals(key)) {
                return Optional.of(feature);
            }
        }

        return Optional.empty();
    }
}
