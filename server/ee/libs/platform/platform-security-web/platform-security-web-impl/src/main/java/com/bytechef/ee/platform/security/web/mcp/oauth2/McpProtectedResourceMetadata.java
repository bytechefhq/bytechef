/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * RFC 9728 OAuth2 protected-resource metadata for an automation or management MCP endpoint: the resource identifier and
 * the authorization server(s) whose tokens the endpoint accepts (the tenant's external identity providers plus
 * ByteChef's own configured issuers).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI", "EI2"
})
public record McpProtectedResourceMetadata(
    String resource, @JsonProperty("authorization_servers") List<String> authorizationServers) {
}
