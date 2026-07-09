/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * RFC 9728 OAuth2 protected-resource metadata for an embedded MCP endpoint: the resource identifier and the
 * authorization server(s) whose tokens the endpoint accepts (the tenant's identity provider flagged for embedded MCP).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI", "EI2"
})
public record ProtectedResourceMetadata(
    String resource, @JsonProperty("authorization_servers") List<String> authorizationServers) {
}
