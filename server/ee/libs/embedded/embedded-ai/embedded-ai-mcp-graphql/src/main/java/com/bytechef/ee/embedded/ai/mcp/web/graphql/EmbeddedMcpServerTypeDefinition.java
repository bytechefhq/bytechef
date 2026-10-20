/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.web.McpServerTypeDefinition;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class EmbeddedMcpServerTypeDefinition implements McpServerTypeDefinition {

    @Override
    public PlatformType getType() {
        return PlatformType.EMBEDDED;
    }

    @Override
    public boolean isSharedAdministration() {
        return false;
    }

    @Override
    public boolean isSecretKeyReadableByEditors() {
        return true;
    }

    @Override
    public String resolvePath(String secretKey) {
        return "/api/embedded/" + secretKey + "/mcp";
    }
}
