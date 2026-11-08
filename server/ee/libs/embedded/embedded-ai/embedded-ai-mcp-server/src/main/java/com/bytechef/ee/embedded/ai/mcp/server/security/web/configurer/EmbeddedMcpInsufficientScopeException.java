/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import org.springframework.security.core.AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpInsufficientScopeException extends AuthenticationException {

    EmbeddedMcpInsufficientScopeException(String message) {
        super(message);
    }
}
