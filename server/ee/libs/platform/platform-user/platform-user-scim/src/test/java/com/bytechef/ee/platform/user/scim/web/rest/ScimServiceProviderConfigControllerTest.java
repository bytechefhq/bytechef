/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @version ee
 */
class ScimServiceProviderConfigControllerTest {

    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScimServiceProviderConfigController())
        .build();

    @Test
    void testGetServiceProviderConfigAdvertisesSupportedFeatures() throws Exception {
        mockMvc.perform(get("/api/scim/v2/ServiceProviderConfig"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"))
            .andExpect(jsonPath("$.patch.supported").value(true))
            .andExpect(jsonPath("$.bulk.supported").value(false))
            .andExpect(jsonPath("$.bulk.maxOperations").value(0))
            .andExpect(jsonPath("$.filter.supported").value(true))
            .andExpect(jsonPath("$.filter.maxResults").value(200))
            .andExpect(jsonPath("$.changePassword.supported").value(false))
            .andExpect(jsonPath("$.sort.supported").value(false))
            .andExpect(jsonPath("$.etag.supported").value(false))
            .andExpect(jsonPath("$.authenticationSchemes", hasSize(1)))
            .andExpect(jsonPath("$.authenticationSchemes[0].type").value("oauthbearertoken"))
            .andExpect(jsonPath("$.authenticationSchemes[0].specUri").value("https://www.rfc-editor.org/info/rfc6750"));
    }

    @Test
    void testGetResourceTypesListsUserAndGroup() throws Exception {
        mockMvc.perform(get("/api/scim/v2/ResourceTypes"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:api:messages:2.0:ListResponse"))
            .andExpect(jsonPath("$.totalResults").value(2))
            .andExpect(jsonPath("$.Resources", hasSize(2)))
            .andExpect(jsonPath("$.Resources[*].id", containsInAnyOrder("User", "Group")))
            .andExpect(jsonPath("$.Resources[*].endpoint", containsInAnyOrder("/Users", "/Groups")))
            .andExpect(
                jsonPath(
                    "$.Resources[*].schema",
                    containsInAnyOrder(
                        "urn:ietf:params:scim:schemas:core:2.0:User", "urn:ietf:params:scim:schemas:core:2.0:Group")));
    }

    @Test
    void testGetSchemasListsUserAndGroupSchemasAsJson() throws Exception {
        mockMvc.perform(get("/api/scim/v2/Schemas"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalResults").value(2))
            .andExpect(
                jsonPath(
                    "$.Resources[*].id",
                    containsInAnyOrder(
                        "urn:ietf:params:scim:schemas:core:2.0:User", "urn:ietf:params:scim:schemas:core:2.0:Group")))
            .andExpect(jsonPath("$.Resources[*].name", containsInAnyOrder("User", "Group")));
    }
}
