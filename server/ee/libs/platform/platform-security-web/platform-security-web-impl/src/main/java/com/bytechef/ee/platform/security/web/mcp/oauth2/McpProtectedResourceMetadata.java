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
 * @version ee
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI", "EI2"
})
public record McpProtectedResourceMetadata(
    String resource, @JsonProperty("authorization_servers") List<String> authorizationServers,
    @JsonProperty("scopes_supported") List<String> scopesSupported, @JsonProperty("resource_name") String resourceName,
    @JsonProperty("tls_client_certificate_bound_access_tokens") boolean tlsClientCertificateBoundAccessTokens) {
}
