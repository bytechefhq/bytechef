/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.platform.user.scim.facade.ScimGroupFacade;
import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.ee.platform.user.scim.model.ScimMember;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @version ee
 */
class ScimGroupControllerTest {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    private static final long GROUP_ID = 7L;
    private static final String GROUPS_PATH = "/api/scim/v2/Groups";
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final ScimGroupFacade scimGroupFacade = mock(ScimGroupFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
        new ScimGroupController(authorityService, scimGroupFacade))
        .build();

    @Test
    void testListGroupsReturnsAllAuthorities() throws Exception {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(1L, "ROLE_ADMIN"), createAuthority(2L, "ROLE_USER")));

        mockMvc.perform(get(GROUPS_PATH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:api:messages:2.0:ListResponse"))
            .andExpect(jsonPath("$.totalResults").value(2))
            .andExpect(jsonPath("$.startIndex").value(1))
            .andExpect(jsonPath("$.Resources", hasSize(2)))
            .andExpect(jsonPath("$.Resources[0].schemas[0]").value(ScimGroup.SCHEMA))
            .andExpect(jsonPath("$.Resources[0].id").value("1"))
            .andExpect(jsonPath("$.Resources[0].displayName").value("ROLE_ADMIN"))
            .andExpect(jsonPath("$.Resources[0].members", hasSize(0)))
            .andExpect(jsonPath("$.Resources[0].meta.resourceType").value("Group"))
            .andExpect(jsonPath("$.Resources[0].meta.location").value("/api/scim/v2/Groups/1"))
            .andExpect(jsonPath("$.Resources[1].displayName").value("ROLE_USER"));
    }

    @Test
    void testListGroupsFilterByDisplayNameIsCaseInsensitive() throws Exception {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(1L, "ROLE_ADMIN"), createAuthority(2L, "ROLE_USER")));

        mockMvc.perform(get(GROUPS_PATH).param("filter", "displayName eq \"role_user\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.startIndex").value(1))
            .andExpect(jsonPath("$.Resources[0].id").value("2"))
            .andExpect(jsonPath("$.Resources[0].displayName").value("ROLE_USER"));
    }

    @Test
    void testListGroupsAppliesStartIndexAndCountWithRealTotal() throws Exception {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(
                createAuthority(3L, "ROLE_C"), createAuthority(1L, "ROLE_A"), createAuthority(2L, "ROLE_B")));

        mockMvc.perform(
            get(GROUPS_PATH)
                .param("startIndex", "2")
                .param("count", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(3))
            .andExpect(jsonPath("$.startIndex").value(2))
            .andExpect(jsonPath("$.itemsPerPage").value(1))
            .andExpect(jsonPath("$.Resources", hasSize(1)))
            .andExpect(jsonPath("$.Resources[0].id").value("2"));
    }

    @Test
    void testListGroupsWithCountZeroReturnsOnlyTotalResults() throws Exception {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(createAuthority(1L, "ROLE_ADMIN"), createAuthority(2L, "ROLE_USER")));

        mockMvc.perform(get(GROUPS_PATH).param("count", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(2))
            .andExpect(jsonPath("$.itemsPerPage").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));
    }

    @Test
    void testListGroupsWithStartIndexBeyondTotalReturnsNoResources() throws Exception {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(1L, "ROLE_ADMIN")));

        mockMvc.perform(get(GROUPS_PATH).param("startIndex", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.startIndex").value(5))
            .andExpect(jsonPath("$.Resources", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "externalId eq \"abc\"",
        "displayName co \"Eng\"",
        "members.value eq \"1\"",
        "displayName eq \"A\" or displayName eq \"B\""
    })
    void testListGroupsRejectsUnsupportedFilterWithInvalidFilterError(String filter) throws Exception {
        mockMvc.perform(get(GROUPS_PATH).param("filter", filter))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.scimType").value("invalidFilter"))
            .andExpect(jsonPath("$.status").value("400"));

        verifyNoInteractions(authorityService);
    }

    @Test
    void testListGroupsFilterByDisplayNameReturnsEmptyListWhenNoMatch() throws Exception {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(1L, "ROLE_ADMIN")));

        mockMvc.perform(get(GROUPS_PATH).param("filter", "displayName eq \"Engineering\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));
    }

    @Test
    void testGetGroupReturnsScimGroup() throws Exception {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.of(createAuthority(GROUP_ID, "ROLE_DEV")));

        mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("7"))
            .andExpect(jsonPath("$.displayName").value("ROLE_DEV"))
            .andExpect(jsonPath("$.meta.created").value(nullValue()))
            .andExpect(jsonPath("$.meta.location").value("/api/scim/v2/Groups/7"));
    }

    @Test
    void testGetGroupReturnsScimNotFoundError() throws Exception {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.detail").value("Group 7 not found"))
            .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    void testCreateGroupReturnsCreatedScimGroup() throws Exception {
        when(scimGroupFacade.createGroup(any(ScimGroup.class))).thenReturn(createAuthority(GROUP_ID, "ROLE_DEV"));

        mockMvc.perform(
            post(GROUPS_PATH)
                .contentType(SCIM_JSON)
                .content("""
                    {
                      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                      "displayName": "ROLE_DEV",
                      "members": [{"value": "42", "$ref": "/api/scim/v2/Users/42", "display": "John"}]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("7"))
            .andExpect(jsonPath("$.displayName").value("ROLE_DEV"));

        ArgumentCaptor<ScimGroup> scimGroupArgumentCaptor = ArgumentCaptor.forClass(ScimGroup.class);

        verify(scimGroupFacade).createGroup(scimGroupArgumentCaptor.capture());

        ScimGroup scimGroup = scimGroupArgumentCaptor.getValue();

        assertThat(scimGroup.displayName()).isEqualTo("ROLE_DEV");
        assertThat(scimGroup.members()).containsExactly(new ScimMember("42", "/api/scim/v2/Users/42", "John"));
    }

    @Test
    void testReplaceGroupReturnsUpdatedScimGroup() throws Exception {
        when(scimGroupFacade.replaceGroup(eq(GROUP_ID), any(ScimGroup.class)))
            .thenReturn(Optional.of(createAuthority(GROUP_ID, "ROLE_RENAMED")));

        mockMvc.perform(
            put(GROUPS_PATH + "/" + GROUP_ID)
                .contentType(SCIM_JSON)
                .content("{\"displayName\": \"ROLE_RENAMED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("ROLE_RENAMED"));
    }

    @Test
    void testReplaceGroupReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
        when(scimGroupFacade.replaceGroup(eq(GROUP_ID), any(ScimGroup.class))).thenReturn(Optional.empty());

        mockMvc.perform(
            put(GROUPS_PATH + "/" + GROUP_ID)
                .contentType(SCIM_JSON)
                .content("{\"displayName\": \"ROLE_RENAMED\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Group 7 not found"));
    }

    @Test
    void testPatchGroupIsNotImplemented() throws Exception {
        mockMvc.perform(
            patch(GROUPS_PATH + "/" + GROUP_ID)
                .contentType(SCIM_JSON)
                .content("{\"Operations\": [{\"op\": \"add\", \"path\": \"members\", \"value\": []}]}"))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.detail").value("PATCH operation is not supported for Group resources"))
            .andExpect(jsonPath("$.status").value("501"));

        verifyNoInteractions(scimGroupFacade);
    }

    @Test
    void testDeleteGroupReturnsNoContent() throws Exception {
        when(scimGroupFacade.deleteGroup(GROUP_ID)).thenReturn(true);

        mockMvc.perform(delete(GROUPS_PATH + "/" + GROUP_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteGroupReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
        when(scimGroupFacade.deleteGroup(anyLong())).thenReturn(false);

        mockMvc.perform(delete(GROUPS_PATH + "/" + GROUP_ID))
            .andExpect(status().isNotFound());
    }

    private static Authority createAuthority(long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }
}
