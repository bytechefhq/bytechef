/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.facade.ScimGroupFacade;
import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.ee.platform.user.scim.model.ScimMember;
import com.bytechef.ee.platform.user.scim.web.rest.config.ScimRestTestConfiguration;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
@WebMvcTest(controllers = ScimGroupController.class, properties = "bytechef.edition=ee")
class ScimGroupControllerIntTest {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    private static final long GROUP_ID = 7L;
    private static final String GROUPS_PATH = "/api/scim/v2/Groups";
    private static final String LIST_RESPONSE_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");

    @MockitoBean
    private AuthorityService authorityService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScimGroupFacade scimGroupFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testListGroupsReturnsAllAuthoritiesAsListResponse() {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(1L, "ROLE_ADMIN"), createAuthority(2L, "ROLE_USER")));

        webTestClient
            .get()
            .uri(GROUPS_PATH)
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
            .jsonPath("$.startIndex")
            .isEqualTo(1)
            .jsonPath("$.Resources.length()")
            .isEqualTo(2)
            .jsonPath("$.Resources[0].schemas[0]")
            .isEqualTo(ScimGroup.SCHEMA)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("1")
            .jsonPath("$.Resources[0].displayName")
            .isEqualTo("ROLE_ADMIN")
            .jsonPath("$.Resources[0].members.length()")
            .isEqualTo(0)
            .jsonPath("$.Resources[0].meta.resourceType")
            .isEqualTo("Group")
            .jsonPath("$.Resources[0].meta.location")
            .isEqualTo("/api/scim/v2/Groups/1")
            .jsonPath("$.Resources[1].displayName")
            .isEqualTo("ROLE_USER");
    }

    @Test
    void testListGroupsFiltersByDisplayNameCaseInsensitively() {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(1L, "ROLE_ADMIN"), createAuthority(2L, "ROLE_USER")));

        webTestClient
            .get()
            .uri(GROUPS_PATH + "?filter={filter}", "displayName eq \"role_user\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(1)
            .jsonPath("$.startIndex")
            .isEqualTo(1)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("2")
            .jsonPath("$.Resources[0].displayName")
            .isEqualTo("ROLE_USER");
    }

    @Test
    void testListGroupsAppliesStartIndexAndCountInIdOrderWithTheRealTotal() {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(3L, "ROLE_C"), createAuthority(1L, "ROLE_A"), createAuthority(2L, "ROLE_B")));

        webTestClient
            .get()
            .uri(GROUPS_PATH + "?startIndex=2&count=1")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(3)
            .jsonPath("$.startIndex")
            .isEqualTo(2)
            .jsonPath("$.itemsPerPage")
            .isEqualTo(1)
            .jsonPath("$.Resources.length()")
            .isEqualTo(1)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("2");
    }

    @Test
    void testListGroupsWithCountZeroReturnsOnlyTheTotal() {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(1L, "ROLE_ADMIN"), createAuthority(2L, "ROLE_USER")));

        webTestClient
            .get()
            .uri(GROUPS_PATH + "?count=0")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(2)
            .jsonPath("$.itemsPerPage")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);
    }

    @Test
    void testListGroupsWithStartIndexBeyondTheTotalReturnsNoResources() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(1L, "ROLE_ADMIN")));

        webTestClient
            .get()
            .uri(GROUPS_PATH + "?startIndex=5")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(1)
            .jsonPath("$.startIndex")
            .isEqualTo(5)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "externalId eq \"abc\"",
        "displayName co \"Eng\"",
        "members.value eq \"1\"",
        "displayName eq \"A\" or displayName eq \"B\""
    })
    void testListGroupsRejectsAnUnsupportedFilterWithInvalidFilter(String filter) {
        webTestClient
            .get()
            .uri(GROUPS_PATH + "?filter={filter}", filter)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.scimType")
            .isEqualTo("invalidFilter")
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(authorityService);
    }

    @Test
    void testListGroupsFilterReturnsAnEmptyListWhenNothingMatches() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(1L, "ROLE_ADMIN")));

        webTestClient
            .get()
            .uri(GROUPS_PATH + "?filter={filter}", "displayName eq \"Engineering\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);
    }

    @Test
    void testGetGroupReturnsTheScimGroup() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.of(createAuthority(GROUP_ID, "ROLE_DEV")));

        webTestClient
            .get()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo("7")
            .jsonPath("$.displayName")
            .isEqualTo("ROLE_DEV")
            .jsonPath("$.meta.created")
            .doesNotExist()
            .jsonPath("$.meta.location")
            .isEqualTo("/api/scim/v2/Groups/7");
    }

    @Test
    void testGetGroupReturnsAScimNotFoundError() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.empty());

        webTestClient
            .get()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.detail")
            .isEqualTo("Group 7 not found")
            .jsonPath("$.status")
            .isEqualTo("404");
    }

    @Test
    void testCreateGroupReturnsTheCreatedScimGroup() {
        when(scimGroupFacade.createGroup(any(ScimGroup.class))).thenReturn(createAuthority(GROUP_ID, "ROLE_DEV"));

        webTestClient
            .post()
            .uri(GROUPS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(
                """
                    {
                      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                      "displayName": "ROLE_DEV",
                      "members": [{"value": "42", "$ref": "/api/scim/v2/Users/42", "display": "John"}]
                    }
                    """)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo("7")
            .jsonPath("$.displayName")
            .isEqualTo("ROLE_DEV");

        ArgumentCaptor<ScimGroup> scimGroupArgumentCaptor = ArgumentCaptor.forClass(ScimGroup.class);

        verify(scimGroupFacade).createGroup(scimGroupArgumentCaptor.capture());

        ScimGroup scimGroup = scimGroupArgumentCaptor.getValue();

        assertThat(scimGroup.displayName()).isEqualTo("ROLE_DEV");
        assertThat(scimGroup.members()).containsExactly(new ScimMember("42", "/api/scim/v2/Users/42", "John"));
    }

    @Test
    void testReplaceGroupReturnsTheUpdatedScimGroup() {
        when(scimGroupFacade.replaceGroup(eq(GROUP_ID), any(ScimGroup.class)))
            .thenReturn(Optional.of(createAuthority(GROUP_ID, "ROLE_RENAMED")));

        webTestClient
            .put()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .contentType(SCIM_JSON)
            .bodyValue("{\"displayName\": \"ROLE_RENAMED\"}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.displayName")
            .isEqualTo("ROLE_RENAMED");
    }

    @Test
    void testReplaceGroupReturnsNotFoundWhenTheGroupDoesNotExist() {
        when(scimGroupFacade.replaceGroup(eq(GROUP_ID), any(ScimGroup.class))).thenReturn(Optional.empty());

        webTestClient
            .put()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .contentType(SCIM_JSON)
            .bodyValue("{\"displayName\": \"ROLE_RENAMED\"}")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("Group 7 not found");
    }

    @Test
    void testCreateGroupRejectsADisplayNameLongerThan50CharactersWithInvalidValue() {
        webTestClient
            .post()
            .uri(GROUPS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"displayName\": \"" + "G".repeat(51) + "\"}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.scimType")
            .isEqualTo("invalidValue")
            .jsonPath("$.detail")
            .isEqualTo("displayName must not be longer than 50 characters")
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimGroupFacade, authorityService);
    }

    @Test
    void testCreateGroupRejectsAMissingDisplayNameWithInvalidValue() {
        webTestClient
            .post()
            .uri(GROUPS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"schemas\": [\"urn:ietf:params:scim:schemas:core:2.0:Group\"]}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.scimType")
            .isEqualTo("invalidValue")
            .jsonPath("$.detail")
            .isEqualTo("displayName is required");

        verifyNoInteractions(scimGroupFacade, authorityService);
    }

    @Test
    void testReplaceGroupRejectsADisplayNameLongerThan50CharactersWithInvalidValue() {
        webTestClient
            .put()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .contentType(SCIM_JSON)
            .bodyValue("{\"displayName\": \"" + "G".repeat(51) + "\"}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.scimType")
            .isEqualTo("invalidValue")
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimGroupFacade, authorityService);
    }

    @Test
    void testPatchGroupIsNotImplemented() {
        webTestClient
            .patch()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .contentType(SCIM_JSON)
            .bodyValue("{\"Operations\": [{\"op\": \"add\", \"path\": \"members\", \"value\": []}]}")
            .exchange()
            .expectStatus()
            .isEqualTo(501)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.detail")
            .isEqualTo("PATCH operation is not supported for Group resources")
            .jsonPath("$.status")
            .isEqualTo("501");

        verifyNoInteractions(scimGroupFacade);
    }

    @Test
    void testDeleteGroupReturnsNoContent() {
        when(scimGroupFacade.deleteGroup(GROUP_ID)).thenReturn(true);

        webTestClient
            .delete()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .exchange()
            .expectStatus()
            .isNoContent();
    }

    @Test
    void testDeleteGroupReturnsNotFoundWhenTheGroupDoesNotExist() {
        when(scimGroupFacade.deleteGroup(anyLong())).thenReturn(false);

        webTestClient
            .delete()
            .uri(GROUPS_PATH + "/" + GROUP_ID)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    private static Authority createAuthority(long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }
}
