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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.platform.user.scim.facade.ScimUserFacade;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @version ee
 */
class ScimUserControllerTest {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    private static final String LIST_RESPONSE_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    private static final long USER_ID = 42L;
    private static final String USERS_PATH = "/api/scim/v2/Users";

    private final ScimUserFacade scimUserFacade = mock(ScimUserFacade.class);
    private final UserService userService = mock(UserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScimUserController(scimUserFacade, userService))
        .build();

    @Test
    void testListUsersReturnsActiveUsersAsListResponse() throws Exception {
        User firstUser = createUser(1L, "john.doe", "john@example.com", true);
        User secondUser = createUser(2L, "jane.doe", "jane@example.com", true);

        when(userService.getAllActiveUsers(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(firstUser, secondUser), PageRequest.of(0, 100), 2));

        mockMvc.perform(get(USERS_PATH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value(LIST_RESPONSE_SCHEMA))
            .andExpect(jsonPath("$.totalResults").value(2))
            .andExpect(jsonPath("$.startIndex").value(1))
            .andExpect(jsonPath("$.itemsPerPage").value(2))
            .andExpect(jsonPath("$.Resources", hasSize(2)))
            .andExpect(jsonPath("$.Resources[0].id").value("1"))
            .andExpect(jsonPath("$.Resources[0].userName").value("john.doe"))
            .andExpect(jsonPath("$.Resources[1].id").value("2"))
            .andExpect(jsonPath("$.Resources[1].userName").value("jane.doe"));

        verify(userService).getAllActiveUsers(new ScimOffsetPageRequest(0, 100));
    }

    @Test
    void testListUsersTranslatesStartIndexAndCountToItemOffset() throws Exception {
        when(userService.getAllActiveUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(
            get(USERS_PATH)
                .param("startIndex", "21")
                .param("count", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startIndex").value(21))
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));

        verify(userService).getAllActiveUsers(new ScimOffsetPageRequest(20, 10));
    }

    @Test
    void testListUsersClampsStartIndexBelowOneToFirstPage() throws Exception {
        when(userService.getAllActiveUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(
            get(USERS_PATH)
                .param("startIndex", "0")
                .param("count", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startIndex").value(1));

        verify(userService).getAllActiveUsers(new ScimOffsetPageRequest(0, 5));
    }

    @Test
    void testListUsersUsesItemOffsetWhenStartIndexIsNotAlignedToCount() throws Exception {
        when(userService.getAllActiveUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(
            get(USERS_PATH)
                .param("startIndex", "50")
                .param("count", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startIndex").value(50));

        ArgumentCaptor<Pageable> pageableArgumentCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userService).getAllActiveUsers(pageableArgumentCaptor.capture());

        Pageable pageable = pageableArgumentCaptor.getValue();

        assertThat(pageable.getOffset()).isEqualTo(49L);
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void testListUsersReportsTotalResultsAcrossAllPages() throws Exception {
        User firstUser = createUser(1L, "john.doe", "john@example.com", true);
        User secondUser = createUser(2L, "jane.doe", "jane@example.com", true);

        when(userService.getAllActiveUsers(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(firstUser, secondUser), new ScimOffsetPageRequest(0, 2), 5));

        mockMvc.perform(get(USERS_PATH).param("count", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(5))
            .andExpect(jsonPath("$.itemsPerPage").value(2))
            .andExpect(jsonPath("$.Resources", hasSize(2)));
    }

    @Test
    void testListUsersWithCountZeroReturnsOnlyTotalResults() throws Exception {
        when(userService.countActiveUsers()).thenReturn(7L);

        mockMvc.perform(get(USERS_PATH).param("count", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(7))
            .andExpect(jsonPath("$.itemsPerPage").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersTreatsNegativeCountAsZero() throws Exception {
        when(userService.countActiveUsers()).thenReturn(3L);

        mockMvc.perform(get(USERS_PATH).param("count", "-5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(3))
            .andExpect(jsonPath("$.Resources", hasSize(0)));

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersCapsCountAtMaxResults() throws Exception {
        when(userService.getAllActiveUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(USERS_PATH).param("count", "1000"))
            .andExpect(status().isOk());

        verify(userService).getAllActiveUsers(new ScimOffsetPageRequest(0, ScimPagination.MAX_RESULTS));
    }

    @Test
    void testListUsersFilterByUserNameReturnsMatchingUser() throws Exception {
        User user = createUser(USER_ID, "john.doe", "john@example.com", true);

        when(userService.fetchUserByLogin("john.doe")).thenReturn(Optional.of(user));

        mockMvc.perform(get(USERS_PATH).param("filter", "userName eq \"john.doe\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schemas[0]").value(LIST_RESPONSE_SCHEMA))
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.startIndex").value(1))
            .andExpect(jsonPath("$.Resources", hasSize(1)))
            .andExpect(jsonPath("$.Resources[0].id").value("42"))
            .andExpect(jsonPath("$.Resources[0].userName").value("john.doe"));

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersFilterByUserNameMatchesCaseInsensitively() throws Exception {
        User user = createUser(USER_ID, "john.doe@x.com", "john.doe@x.com", true);

        when(userService.fetchUserByLogin("john.doe@x.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get(USERS_PATH).param("filter", "userName eq \"John.Doe@X.com\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.Resources[0].id").value("42"))
            .andExpect(jsonPath("$.Resources[0].userName").value("john.doe@x.com"));

        verify(userService).fetchUserByLogin("john.doe@x.com");
        verify(userService, never()).fetchUserByLogin("John.Doe@X.com");
    }

    @Test
    void testListUsersFilterAppliesStartIndexToTheMatchedUser() throws Exception {
        when(userService.fetchUserByLogin("john.doe"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));

        mockMvc.perform(
            get(USERS_PATH)
                .param("filter", "userName eq \"john.doe\"")
                .param("startIndex", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.startIndex").value(3))
            .andExpect(jsonPath("$.itemsPerPage").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));
    }

    @Test
    void testListUsersFilterWithCountZeroReturnsOnlyTotalResults() throws Exception {
        when(userService.fetchUserByLogin("john.doe"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));

        mockMvc.perform(
            get(USERS_PATH)
                .param("filter", "userName eq \"john.doe\"")
                .param("count", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.Resources", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "emails.value eq \"a@b.com\"",
        "userName eq \"x\" and active eq true",
        "userName co \"x\"",
        "active eq true",
        "displayName eq \"John Doe\""
    })
    void testListUsersRejectsUnsupportedFilterWithInvalidFilterError(String filter) throws Exception {
        mockMvc.perform(get(USERS_PATH).param("filter", filter))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.scimType").value("invalidFilter"))
            .andExpect(jsonPath("$.status").value("400"));

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
        verify(userService, never()).fetchUserByLogin(anyString());
        verify(userService, never()).fetchUserByEmail(anyString());
    }

    @Test
    void testListUsersTreatsBlankFilterAsNoFilter() throws Exception {
        when(userService.getAllActiveUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(USERS_PATH).param("filter", " "))
            .andExpect(status().isOk());

        verify(userService).getAllActiveUsers(new ScimOffsetPageRequest(0, 100));
    }

    @Test
    void testListUsersFilterAttributeNameIsCaseInsensitive() throws Exception {
        when(userService.fetchUserByLogin("john.doe"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));

        mockMvc.perform(get(USERS_PATH).param("filter", "USERNAME eq \"john.doe\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1));

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersFilterByUserNameReturnsEmptyListWhenNoMatch() throws Exception {
        when(userService.fetchUserByLogin("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get(USERS_PATH).param("filter", "userName eq \"unknown\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.itemsPerPage").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersFilterByExternalIdLooksUpUserByEmail() throws Exception {
        User user = createUser(USER_ID, "john.doe", "john@example.com", true);

        when(userService.fetchUserByEmail("john@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get(USERS_PATH).param("filter", "externalId eq \"john@example.com\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.Resources[0].externalId").value("john@example.com"));

        verify(userService, never()).fetchUserByLogin(anyString());
        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersFilterByExternalIdReturnsEmptyListWhenNoMatch() throws Exception {
        when(userService.fetchUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get(USERS_PATH).param("filter", "externalId eq \"nobody@example.com\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.Resources", hasSize(0)));
    }

    @Test
    void testGetUserMapsUserToScimResource() throws Exception {
        User user = createUser(USER_ID, "john.doe", "john@example.com", true);

        user.setCreatedDate(Instant.parse("2025-01-02T03:04:05Z"));

        ReflectionTestUtils.setField(user, "lastModifiedDate", Instant.parse("2025-02-03T04:05:06Z"));

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        mockMvc.perform(get(USERS_PATH + "/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value(ScimUser.SCHEMA))
            .andExpect(jsonPath("$.id").value("42"))
            .andExpect(jsonPath("$.externalId").value("john@example.com"))
            .andExpect(jsonPath("$.userName").value("john.doe"))
            .andExpect(jsonPath("$.name.formatted").value("John Doe"))
            .andExpect(jsonPath("$.name.givenName").value("John"))
            .andExpect(jsonPath("$.name.familyName").value("Doe"))
            .andExpect(jsonPath("$.displayName").value("John Doe"))
            .andExpect(jsonPath("$.emails", hasSize(1)))
            .andExpect(jsonPath("$.emails[0].value").value("john@example.com"))
            .andExpect(jsonPath("$.emails[0].type").value("work"))
            .andExpect(jsonPath("$.emails[0].primary").value(true))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.groups", hasSize(0)))
            .andExpect(jsonPath("$.meta.resourceType").value("User"))
            .andExpect(jsonPath("$.meta.created").value("2025-01-02T03:04:05Z"))
            .andExpect(jsonPath("$.meta.lastModified").value("2025-02-03T04:05:06Z"))
            .andExpect(jsonPath("$.meta.location").value("/api/scim/v2/Users/42"));
    }

    @Test
    void testGetUserMapsMissingOptionalAttributes() throws Exception {
        User user = createUser(USER_ID, "jdoe", null, false);

        user.setFirstName(null);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        mockMvc.perform(get(USERS_PATH + "/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name.formatted").value("Doe"))
            .andExpect(jsonPath("$.name.givenName").value(nullValue()))
            .andExpect(jsonPath("$.name.familyName").value("Doe"))
            .andExpect(jsonPath("$.displayName").value("Doe"))
            .andExpect(jsonPath("$.emails", hasSize(0)))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.meta.created").value(nullValue()))
            .andExpect(jsonPath("$.meta.lastModified").value(nullValue()));
    }

    @Test
    void testGetUserReturnsScimNotFoundError() throws Exception {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(USERS_PATH + "/" + USER_ID))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.detail").value("User 42 not found"))
            .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    void testCreateUserReturnsCreatedScimUser() throws Exception {
        when(scimUserFacade.createUser(any(ScimUser.class)))
            .thenReturn(createUser(USER_ID, "john.doe", "john@example.com", true));

        mockMvc.perform(
            post(USERS_PATH)
                .contentType(SCIM_JSON)
                .content("""
                    {
                      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                      "externalId": "ext-1",
                      "userName": "john.doe",
                      "name": {"givenName": "John", "familyName": "Doe"},
                      "emails": [{"value": "john@example.com", "type": "work", "primary": true}],
                      "active": true
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.id").value("42"))
            .andExpect(jsonPath("$.userName").value("john.doe"))
            .andExpect(jsonPath("$.meta.location").value("/api/scim/v2/Users/42"));

        ArgumentCaptor<ScimUser> scimUserArgumentCaptor = ArgumentCaptor.forClass(ScimUser.class);

        verify(scimUserFacade).createUser(scimUserArgumentCaptor.capture());

        ScimUser scimUser = scimUserArgumentCaptor.getValue();

        assertThat(scimUser.externalId()).isEqualTo("ext-1");
        assertThat(scimUser.userName()).isEqualTo("john.doe");
        assertThat(scimUser.name()).isEqualTo(new ScimName(null, "Doe", "John"));
        assertThat(scimUser.emails()).containsExactly(new ScimEmail("john@example.com", "work", true));
        assertThat(scimUser.active()).isTrue();
    }

    @Test
    void testCreateUserReturnsUniquenessErrorWhenUserNameIsTaken() throws Exception {
        when(scimUserFacade.createUser(any(ScimUser.class)))
            .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint \"ux_user_login\""));

        mockMvc.perform(
            post(USERS_PATH)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"john.doe\", \"active\": true}"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.scimType").value("uniqueness"))
            .andExpect(jsonPath("$.status").value("409"));
    }

    @Test
    void testReplaceUserReturnsUniquenessErrorWhenUserNameIsTaken() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class)))
            .thenThrow(new DuplicateKeyException("duplicate key"));

        mockMvc.perform(
            put(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"jane.doe\", \"active\": true}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.scimType").value("uniqueness"));
    }

    @Test
    void testReplaceUserReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(
            put(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"john.doe\", \"active\": true}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.detail").value("User 42 not found"))
            .andExpect(jsonPath("$.status").value("404"));

        verify(scimUserFacade, never()).replaceUser(anyLong(), any(ScimUser.class));
        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserDelegatesToReplaceWhenUserStaysActive() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.smith", "john@example.com", true)));

        mockMvc.perform(
            put(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"john.smith\", \"active\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("john.smith"))
            .andExpect(jsonPath("$.active").value(true));

        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserDelegatesToDeactivateWhenActiveUserBecomesInactive() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimUser.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        mockMvc.perform(
            put(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"john.doe\", \"active\": false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        verify(scimUserFacade, never()).replaceUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserDelegatesToReplaceWhenUserIsAlreadyInactive() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        mockMvc.perform(
            put(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"john.doe\", \"active\": false}"))
            .andExpect(status().isOk());

        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserReturnsNotFoundWhenFacadeFindsNoUser() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class))).thenReturn(Optional.empty());

        mockMvc.perform(
            put(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content("{\"userName\": \"john.doe\", \"active\": true}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("User 42 not found"));
    }

    @Test
    void testPatchUserReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(
            patch(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content(patchBody("\"op\": \"replace\", \"path\": \"active\", \"value\": false")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA))
            .andExpect(jsonPath("$.status").value("404"));

        verify(scimUserFacade, never()).patchUser(anyLong(), any(ScimPatchRequest.class));
        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimPatchRequest.class));
    }

    @Test
    void testPatchUserDelegatesToDeactivateForActiveFalseOperation() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        mockMvc.perform(
            patch(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content(patchBody("\"op\": \"Replace\", \"path\": \"active\", \"value\": \"False\"")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        ArgumentCaptor<ScimPatchRequest> scimPatchRequestArgumentCaptor =
            ArgumentCaptor.forClass(ScimPatchRequest.class);

        verify(scimUserFacade).deactivateUser(eq(USER_ID), scimPatchRequestArgumentCaptor.capture());
        verify(scimUserFacade, never()).patchUser(anyLong(), any(ScimPatchRequest.class));

        ScimPatchRequest scimPatchRequest = scimPatchRequestArgumentCaptor.getValue();

        assertThat(scimPatchRequest.operations()).containsExactly(
            new ScimPatchOperation("Replace", "active", "False"));
    }

    @Test
    void testPatchUserDelegatesToDeactivateForPathlessActiveValueMap() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        mockMvc.perform(
            patch(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content(patchBody("\"op\": \"replace\", \"value\": {\"active\": false}")))
            .andExpect(status().isOk());

        ArgumentCaptor<ScimPatchRequest> scimPatchRequestArgumentCaptor =
            ArgumentCaptor.forClass(ScimPatchRequest.class);

        verify(scimUserFacade).deactivateUser(eq(USER_ID), scimPatchRequestArgumentCaptor.capture());

        ScimPatchRequest scimPatchRequest = scimPatchRequestArgumentCaptor.getValue();

        assertThat(scimPatchRequest.operations()).containsExactly(
            new ScimPatchOperation("replace", null, Map.of("active", false)));
    }

    @Test
    void testPatchUserDelegatesToPatchForAttributeChanges() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.patchUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "jane.doe", "john@example.com", true)));

        mockMvc.perform(
            patch(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content(patchBody("\"op\": \"replace\", \"path\": \"userName\", \"value\": \"jane.doe\"")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("jane.doe"));

        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimPatchRequest.class));
    }

    @Test
    void testPatchUserReturnsNotFoundWhenFacadeFindsNoUser() throws Exception {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.patchUser(eq(USER_ID), any(ScimPatchRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(
            patch(USERS_PATH + "/" + USER_ID)
                .contentType(SCIM_JSON)
                .content(patchBody("\"op\": \"replace\", \"path\": \"userName\", \"value\": \"jane.doe\"")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("User 42 not found"));
    }

    @Test
    void testDeleteUserReturnsNoContent() throws Exception {
        when(scimUserFacade.deleteUser(USER_ID)).thenReturn(true);

        mockMvc.perform(delete(USERS_PATH + "/" + USER_ID))
            .andExpect(status().isNoContent());

        verify(scimUserFacade).deleteUser(USER_ID);
    }

    @Test
    void testDeleteUserReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(scimUserFacade.deleteUser(USER_ID)).thenReturn(false);

        mockMvc.perform(delete(USERS_PATH + "/" + USER_ID))
            .andExpect(status().isNotFound());
    }

    private static String patchBody(String operation) {
        return "{\"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"], \"Operations\": [{" + operation +
            "}]}";
    }

    private static User createUser(long id, String login, String email, boolean activated) {
        User user = new User();

        user.setActivated(activated);
        user.setEmail(email);
        user.setFirstName("John");
        user.setId(id);
        user.setLastName("Doe");
        user.setLogin(login);

        return user;
    }
}
