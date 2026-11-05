/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;

import com.bytechef.ee.platform.user.scim.web.rest.config.ScimRestTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = ScimRestTestConfiguration.class)
@WebMvcTest(controllers = ScimServiceProviderConfigController.class, properties = "bytechef.edition=ee")
class ScimServiceProviderConfigControllerIntTest {

    private static final String GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
    private static final String LIST_RESPONSE_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testGetServiceProviderConfigAdvertisesTheSupportedFeatures() {
        webTestClient
            .get()
            .uri("/api/scim/v2/ServiceProviderConfig")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig")
            .jsonPath("$.patch.supported")
            .isEqualTo(true)
            .jsonPath("$.bulk.supported")
            .isEqualTo(false)
            .jsonPath("$.bulk.maxOperations")
            .isEqualTo(0)
            .jsonPath("$.filter.supported")
            .isEqualTo(true)
            .jsonPath("$.filter.maxResults")
            .isEqualTo(ScimPagination.MAX_RESULTS)
            .jsonPath("$.changePassword.supported")
            .isEqualTo(false)
            .jsonPath("$.sort.supported")
            .isEqualTo(false)
            .jsonPath("$.etag.supported")
            .isEqualTo(false)
            .jsonPath("$.authenticationSchemes.length()")
            .isEqualTo(1)
            .jsonPath("$.authenticationSchemes[0].type")
            .isEqualTo("oauthbearertoken")
            .jsonPath("$.authenticationSchemes[0].specUri")
            .isEqualTo("https://www.rfc-editor.org/info/rfc6750");
    }

    @Test
    void testGetResourceTypesListsTheUserAndGroupResourceTypes() {
        webTestClient
            .get()
            .uri("/api/scim/v2/ResourceTypes")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(LIST_RESPONSE_SCHEMA)
            .jsonPath("$.totalResults")
            .isEqualTo(2)
            .jsonPath("$.Resources.length()")
            .isEqualTo(2)
            .jsonPath("$.Resources[*].id")
            .value(containsInAnyOrder("User", "Group"))
            .jsonPath("$.Resources[*].endpoint")
            .value(containsInAnyOrder("/Users", "/Groups"))
            .jsonPath("$.Resources[*].schema")
            .value(containsInAnyOrder(USER_SCHEMA, GROUP_SCHEMA));
    }

    @Test
    void testGetSchemasListsTheUserAndGroupSchemasAsJson() {
        webTestClient
            .get()
            .uri("/api/scim/v2/Schemas")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(2)
            .jsonPath("$.Resources[*].id")
            .value(containsInAnyOrder(USER_SCHEMA, GROUP_SCHEMA))
            .jsonPath("$.Resources[*].name")
            .value(containsInAnyOrder("User", "Group"));
    }
}
