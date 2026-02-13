/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCIM 2.0 discovery endpoints (RFC 7644 Section 4).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(path = "/api/scim/v2", produces = "application/scim+json")
class ScimServiceProviderConfigController {

    @GetMapping("/ServiceProviderConfig")
    Map<String, Object> getServiceProviderConfig() {
        return Map.of(
            "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"),
            "documentationUri", "https://www.bytechef.io/docs/scim",
            "patch", Map.of("supported", true),
            "bulk", Map.of("supported", false, "maxOperations", 0, "maxPayloadSize", 0),
            "filter", Map.of("supported", true, "maxResults", 200),
            "changePassword", Map.of("supported", false),
            "sort", Map.of("supported", false),
            "etag", Map.of("supported", false),
            "authenticationSchemes", List.of(
                Map.of(
                    "type", "oauthbearertoken",
                    "name", "OAuth Bearer Token",
                    "description", "Authentication scheme using the OAuth Bearer Token Standard",
                    "specUri", "https://www.rfc-editor.org/info/rfc6750",
                    "documentationUri", "https://www.bytechef.io/docs/scim/auth")));
    }

    @GetMapping("/ResourceTypes")
    Map<String, Object> getResourceTypes() {
        return Map.of(
            "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
            "totalResults", 2,
            "Resources", List.of(
                Map.of(
                    "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                    "id", "User",
                    "name", "User",
                    "endpoint", "/Users",
                    "schema", "urn:ietf:params:scim:schemas:core:2.0:User"),
                Map.of(
                    "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                    "id", "Group",
                    "name", "Group",
                    "endpoint", "/Groups",
                    "schema", "urn:ietf:params:scim:schemas:core:2.0:Group")));
    }

    @GetMapping(value = "/Schemas", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> getSchemas() {
        return Map.of(
            "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
            "totalResults", 2,
            "Resources", List.of(
                Map.of(
                    "id", "urn:ietf:params:scim:schemas:core:2.0:User",
                    "name", "User",
                    "description", "User Account"),
                Map.of(
                    "id", "urn:ietf:params:scim:schemas:core:2.0:Group",
                    "name", "Group",
                    "description", "Group")));
    }
}
