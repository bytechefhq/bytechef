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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.facade.ScimUserFacade;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.ee.platform.user.scim.web.rest.config.ScimRestTestConfiguration;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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
@WebMvcTest(controllers = ScimUserController.class, properties = "bytechef.edition=ee")
class ScimUserControllerIntTest {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    private static final String LIST_RESPONSE_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    private static final long USER_ID = 42L;
    private static final String USER_PATH = "/api/scim/v2/Users/42";
    private static final String USERS_PATH = "/api/scim/v2/Users";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScimUserFacade scimUserFacade;

    @MockitoBean
    private UserService userService;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testListUsersReturnsTheUsersAsListResponse() {
        User firstUser = createUser(1L, "john.doe", "john@example.com", true);
        User secondUser = createUser(2L, "jane.doe", "jane@example.com", true);

        when(userService.getAllManagedUsers(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(firstUser, secondUser), new ScimOffsetPageRequest(0, 100), 2));

        webTestClient
            .get()
            .uri(USERS_PATH)
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
            .jsonPath("$.itemsPerPage")
            .isEqualTo(2)
            .jsonPath("$.Resources.length()")
            .isEqualTo(2)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("1")
            .jsonPath("$.Resources[0].userName")
            .isEqualTo("john.doe")
            .jsonPath("$.Resources[1].id")
            .isEqualTo("2")
            .jsonPath("$.Resources[1].userName")
            .isEqualTo("jane.doe");

        verify(userService).getAllManagedUsers(new ScimOffsetPageRequest(0, 100));
    }

    @Test
    void testListUsersIncludesDeactivatedUsers() {
        User activeUser = createUser(1L, "john.doe", "john@example.com", true);
        User deactivatedUser = createUser(2L, "jane.doe", "jane@example.com", false);

        when(userService.getAllManagedUsers(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(activeUser, deactivatedUser), new ScimOffsetPageRequest(0, 100), 2));

        webTestClient
            .get()
            .uri(USERS_PATH)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(2)
            .jsonPath("$.Resources[0].active")
            .isEqualTo(true)
            .jsonPath("$.Resources[1].userName")
            .isEqualTo("jane.doe")
            .jsonPath("$.Resources[1].active")
            .isEqualTo(false);

        verify(userService, never()).getAllActiveUsers(any(Pageable.class));
    }

    @Test
    void testListUsersTranslatesStartIndexAndCountToAnItemOffset() {
        when(userService.getAllManagedUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        webTestClient
            .get()
            .uri(USERS_PATH + "?startIndex=21&count=10")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.startIndex")
            .isEqualTo(21)
            .jsonPath("$.totalResults")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);

        verify(userService).getAllManagedUsers(new ScimOffsetPageRequest(20, 10));
    }

    @Test
    void testListUsersClampsAStartIndexBelowOneToTheFirstItem() {
        when(userService.getAllManagedUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        webTestClient
            .get()
            .uri(USERS_PATH + "?startIndex=0&count=5")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.startIndex")
            .isEqualTo(1);

        verify(userService).getAllManagedUsers(new ScimOffsetPageRequest(0, 5));
    }

    @Test
    void testListUsersUsesTheItemOffsetWhenStartIndexIsNotAlignedToCount() {
        when(userService.getAllManagedUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        webTestClient
            .get()
            .uri(USERS_PATH + "?startIndex=50&count=100")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.startIndex")
            .isEqualTo(50);

        ArgumentCaptor<Pageable> pageableArgumentCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userService).getAllManagedUsers(pageableArgumentCaptor.capture());

        Pageable pageable = pageableArgumentCaptor.getValue();

        assertThat(pageable.getOffset()).isEqualTo(49L);
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void testListUsersReportsTheTotalResultsAcrossAllPages() {
        User firstUser = createUser(1L, "john.doe", "john@example.com", true);
        User secondUser = createUser(2L, "jane.doe", "jane@example.com", true);

        when(userService.getAllManagedUsers(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(firstUser, secondUser), new ScimOffsetPageRequest(0, 2), 5));

        webTestClient
            .get()
            .uri(USERS_PATH + "?count=2")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(5)
            .jsonPath("$.itemsPerPage")
            .isEqualTo(2)
            .jsonPath("$.Resources.length()")
            .isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0", "-5"
    })
    void testListUsersWithCountZeroOrBelowReturnsOnlyTheTotalOfAllUsers(String count) {
        when(userService.countUsers()).thenReturn(7L);

        webTestClient
            .get()
            .uri(USERS_PATH + "?count=" + count)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(7)
            .jsonPath("$.itemsPerPage")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);

        verify(userService, never()).getAllManagedUsers(any(Pageable.class));
        verify(userService, never()).countActiveUsers();
    }

    @Test
    void testListUsersCapsCountAtMaxResults() {
        when(userService.getAllManagedUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        webTestClient
            .get()
            .uri(USERS_PATH + "?count=1000")
            .exchange()
            .expectStatus()
            .isOk();

        verify(userService).getAllManagedUsers(new ScimOffsetPageRequest(0, ScimPagination.MAX_RESULTS));
    }

    @Test
    void testListUsersFilterByUserNameReturnsTheMatchingUser() {
        when(userService.fetchUserByLogin("john.doe"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", "userName eq \"john.doe\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(LIST_RESPONSE_SCHEMA)
            .jsonPath("$.totalResults")
            .isEqualTo(1)
            .jsonPath("$.startIndex")
            .isEqualTo(1)
            .jsonPath("$.Resources.length()")
            .isEqualTo(1)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("42")
            .jsonPath("$.Resources[0].userName")
            .isEqualTo("john.doe");

        verify(userService, never()).getAllManagedUsers(any(Pageable.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "userName eq \"John.Doe@X.com\"", "USERNAME eq \"john.doe@x.com\""
    })
    void testListUsersFilterByUserNameMatchesCaseInsensitively(String filter) {
        when(userService.fetchUserByLogin("john.doe@x.com"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe@x.com", "john.doe@x.com", true)));

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", filter)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(1)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("42")
            .jsonPath("$.Resources[0].userName")
            .isEqualTo("john.doe@x.com");

        verify(userService).fetchUserByLogin("john.doe@x.com");
        verify(userService, never()).getAllManagedUsers(any(Pageable.class));
    }

    @Test
    void testListUsersFilterAppliesStartIndexAndCountToTheMatchedUsers() {
        when(userService.fetchUserByLogin("john.doe"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}&startIndex=3", "userName eq \"john.doe\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(1)
            .jsonPath("$.startIndex")
            .isEqualTo(3)
            .jsonPath("$.itemsPerPage")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}&count=0", "userName eq \"john.doe\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(1)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "emails.value eq \"a@b.com\"",
        "userName eq \"x\" and active eq true",
        "userName co \"x\"",
        "active eq true",
        "displayName eq \"John Doe\""
    })
    void testListUsersRejectsAnUnsupportedFilterWithInvalidFilter(String filter) {
        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", filter)
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

        verifyNoInteractions(userService);
    }

    @Test
    void testListUsersTreatsABlankFilterAsNoFilter() {
        when(userService.getAllManagedUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", " ")
            .exchange()
            .expectStatus()
            .isOk();

        verify(userService).getAllManagedUsers(new ScimOffsetPageRequest(0, 100));
    }

    @Test
    void testListUsersFilterByUserNameReturnsAnEmptyListWhenNothingMatches() {
        when(userService.fetchUserByLogin("unknown")).thenReturn(Optional.empty());

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", "userName eq \"unknown\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(0)
            .jsonPath("$.itemsPerPage")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);
    }

    @Test
    void testListUsersFilterByExternalIdReturnsEveryUserWithThatExternalId() {
        User firstUser = createUser(1L, "john.doe", "john@example.com", true);
        User secondUser = createUser(2L, "jane.doe", "jane@example.com", false);

        firstUser.setExternalId("00u-shared");
        secondUser.setExternalId("00u-shared");

        when(userService.getUsersByExternalId("00u-shared")).thenReturn(List.of(firstUser, secondUser));

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", "externalId eq \"00u-shared\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(2)
            .jsonPath("$.Resources[0].id")
            .isEqualTo("1")
            .jsonPath("$.Resources[0].externalId")
            .isEqualTo("00u-shared")
            .jsonPath("$.Resources[1].id")
            .isEqualTo("2");

        verify(userService, never()).fetchUserByEmail(anyString());
        verify(userService, never()).fetchUserByLogin(anyString());
        verify(userService, never()).getAllManagedUsers(any(Pageable.class));
    }

    @Test
    void testListUsersFilterByExternalIdDoesNotMatchTheEmail() {
        when(userService.fetchUserByEmail("john@example.com"))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(userService.getUsersByExternalId("john@example.com")).thenReturn(List.of());

        webTestClient
            .get()
            .uri(USERS_PATH + "?filter={filter}", "externalId eq \"john@example.com\"")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.totalResults")
            .isEqualTo(0)
            .jsonPath("$.Resources.length()")
            .isEqualTo(0);

        verify(userService).getUsersByExternalId("john@example.com");
    }

    @Test
    void testGetUserMapsTheUserToAScimResource() {
        User user = createUser(USER_ID, "john.doe", "john@example.com", true);

        user.setCreatedDate(Instant.parse("2025-01-02T03:04:05Z"));
        user.setExternalId("00u-john");

        ReflectionTestUtils.setField(user, "lastModifiedDate", Instant.parse("2025-02-03T04:05:06Z"));

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        webTestClient
            .get()
            .uri(USER_PATH)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ScimUser.SCHEMA)
            .jsonPath("$.id")
            .isEqualTo("42")
            .jsonPath("$.externalId")
            .isEqualTo("00u-john")
            .jsonPath("$.userName")
            .isEqualTo("john.doe")
            .jsonPath("$.name.formatted")
            .isEqualTo("John Doe")
            .jsonPath("$.name.givenName")
            .isEqualTo("John")
            .jsonPath("$.name.familyName")
            .isEqualTo("Doe")
            .jsonPath("$.displayName")
            .isEqualTo("John Doe")
            .jsonPath("$.emails.length()")
            .isEqualTo(1)
            .jsonPath("$.emails[0].value")
            .isEqualTo("john@example.com")
            .jsonPath("$.emails[0].type")
            .isEqualTo("work")
            .jsonPath("$.emails[0].primary")
            .isEqualTo(true)
            .jsonPath("$.active")
            .isEqualTo(true)
            .jsonPath("$.groups.length()")
            .isEqualTo(0)
            .jsonPath("$.meta.resourceType")
            .isEqualTo("User")
            .jsonPath("$.meta.created")
            .isEqualTo("2025-01-02T03:04:05Z")
            .jsonPath("$.meta.lastModified")
            .isEqualTo("2025-02-03T04:05:06Z")
            .jsonPath("$.meta.location")
            .isEqualTo(USER_PATH);
    }

    @Test
    void testGetUserMapsMissingOptionalAttributes() {
        User user = createUser(USER_ID, "jdoe", null, false);

        user.setFirstName(null);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        webTestClient
            .get()
            .uri(USER_PATH)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.externalId")
            .doesNotExist()
            .jsonPath("$.name.formatted")
            .isEqualTo("Doe")
            .jsonPath("$.name.givenName")
            .doesNotExist()
            .jsonPath("$.name.familyName")
            .isEqualTo("Doe")
            .jsonPath("$.displayName")
            .isEqualTo("Doe")
            .jsonPath("$.emails.length()")
            .isEqualTo(0)
            .jsonPath("$.active")
            .isEqualTo(false)
            .jsonPath("$.meta.created")
            .doesNotExist()
            .jsonPath("$.meta.lastModified")
            .doesNotExist();
    }

    @Test
    void testGetUserReturnsAScimNotFoundError() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        webTestClient
            .get()
            .uri(USER_PATH)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.detail")
            .isEqualTo("User 42 not found")
            .jsonPath("$.status")
            .isEqualTo("404");
    }

    @Test
    void testCreateUserReturnsTheCreatedScimUser() {
        when(scimUserFacade.createUser(any(ScimUser.class)))
            .thenReturn(createUser(USER_ID, "john.doe", "john@example.com", true));

        webTestClient
            .post()
            .uri(USERS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(
                """
                    {
                      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                      "externalId": "ext-1",
                      "userName": "john.doe",
                      "name": {"givenName": "John", "familyName": "Doe"},
                      "emails": [{"value": "john@example.com", "type": "work", "primary": true}],
                      "active": true
                    }
                    """)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo("42")
            .jsonPath("$.userName")
            .isEqualTo("john.doe")
            .jsonPath("$.meta.location")
            .isEqualTo(USER_PATH);

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
    void testCreateUserAcceptsAnExternalIdOf255Characters() {
        when(scimUserFacade.createUser(any(ScimUser.class)))
            .thenReturn(createUser(USER_ID, "john.doe", "john@example.com", true));

        webTestClient
            .post()
            .uri(USERS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(userBody("john.doe", "john@example.com", "John", "Doe", "x".repeat(255)))
            .exchange()
            .expectStatus()
            .isCreated();

        verify(scimUserFacade).createUser(any(ScimUser.class));
    }

    @Test
    void testCreateUserReturnsAUniquenessErrorWhenTheUserNameIsTaken() {
        when(scimUserFacade.createUser(any(ScimUser.class)))
            .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint \"ux_user_login\""));

        webTestClient
            .post()
            .uri(USERS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"john.doe\", \"active\": true}")
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.scimType")
            .isEqualTo("uniqueness")
            .jsonPath("$.status")
            .isEqualTo("409");
    }

    @Test
    void testCreateUserRejectsAnExternalIdLongerThan255CharactersWithInvalidValue() {
        webTestClient
            .post()
            .uri(USERS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(userBody("john.doe", "john@example.com", "John", "Doe", "x".repeat(256)))
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
            .isEqualTo("externalId must not be longer than 255 characters")
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimUserFacade, userService);
    }

    @ParameterizedTest
    @MethodSource("unstorableUserBodies")
    void testCreateUserRejectsValuesTheColumnsCannotStoreWithInvalidValue(String body) {
        webTestClient
            .post()
            .uri(USERS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.scimType")
            .isEqualTo("invalidValue")
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimUserFacade, userService);
    }

    @Test
    void testCreateUserRejectsAMissingUserNameWithInvalidValue() {
        webTestClient
            .post()
            .uri(USERS_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"emails\": [{\"value\": \"john@example.com\", \"primary\": true}], \"active\": true}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.scimType")
            .isEqualTo("invalidValue")
            .jsonPath("$.detail")
            .isEqualTo("userName is required");

        verifyNoInteractions(scimUserFacade, userService);
    }

    @Test
    void testReplaceUserReplacesWhenTheUserStaysActive() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.smith", "john@example.com", true)));

        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"john.smith\", \"active\": true}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.userName")
            .isEqualTo("john.smith")
            .jsonPath("$.active")
            .isEqualTo(true);

        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserDeactivatesWhenAnActiveUserBecomesInactive() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimUser.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"john.doe\", \"active\": false}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.active")
            .isEqualTo(false);

        verify(scimUserFacade, never()).replaceUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserReplacesWhenTheUserIsAlreadyInactive() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"john.doe\", \"active\": false}")
            .exchange()
            .expectStatus()
            .isOk();

        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimUser.class));
    }

    @Test
    void testReplaceUserReturnsAScimNotFoundErrorWhenTheUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"john.doe\", \"active\": true}")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.detail")
            .isEqualTo("User 42 not found")
            .jsonPath("$.status")
            .isEqualTo("404");

        verifyNoInteractions(scimUserFacade);
    }

    @Test
    void testReplaceUserReturnsNotFoundWhenTheFacadeFindsNoUser() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class))).thenReturn(Optional.empty());

        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"john.doe\", \"active\": true}")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("User 42 not found");
    }

    @Test
    void testReplaceUserReturnsAUniquenessErrorWhenTheUserNameIsTaken() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.replaceUser(eq(USER_ID), any(ScimUser.class)))
            .thenThrow(new DuplicateKeyException("duplicate key"));

        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue("{\"userName\": \"jane.doe\", \"active\": true}")
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.scimType")
            .isEqualTo("uniqueness");
    }

    @Test
    void testReplaceUserRejectsAnExternalIdLongerThan255CharactersWithInvalidValue() {
        webTestClient
            .put()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(userBody("john.doe", "john@example.com", "John", "Doe", "x".repeat(256)))
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
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimUserFacade, userService);
    }

    @Test
    void testPatchUserReturnsAScimNotFoundErrorWhenTheUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"replace\", \"path\": \"active\", \"value\": false"))
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.status")
            .isEqualTo("404");

        verifyNoInteractions(scimUserFacade);
    }

    @Test
    void testPatchUserDeactivatesForAnActiveFalseOperation() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"Replace\", \"path\": \"active\", \"value\": \"False\""))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.active")
            .isEqualTo(false);

        assertThat(captureDeactivationPatchRequest().operations())
            .containsExactly(new ScimPatchOperation("Replace", "active", "False"));

        verify(scimUserFacade, never()).patchUser(anyLong(), any(ScimPatchRequest.class));
    }

    @Test
    void testPatchUserDeactivatesForAPathlessActiveFalseValue() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"replace\", \"value\": {\"active\": false}"))
            .exchange()
            .expectStatus()
            .isOk();

        assertThat(captureDeactivationPatchRequest().operations())
            .containsExactly(new ScimPatchOperation("replace", null, Map.of("active", false)));
    }

    @Test
    void testPatchUserDeactivatesForAnAzureStyleAddOfActiveFalse() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.deactivateUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", false)));

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"Add\", \"value\": {\"active\": false}"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.active")
            .isEqualTo(false);

        verify(scimUserFacade).deactivateUser(eq(USER_ID), any(ScimPatchRequest.class));
        verify(scimUserFacade, never()).patchUser(anyLong(), any(ScimPatchRequest.class));
    }

    @Test
    void testPatchUserPatchesForAttributeChanges() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.patchUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "jane.doe", "john@example.com", true)));

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"replace\", \"path\": \"userName\", \"value\": \"jane.doe\""))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.userName")
            .isEqualTo("jane.doe");

        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimPatchRequest.class));
    }

    @Test
    void testPatchUserPatchesForAnAzureStyleAddOfTheExternalId() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.patchUser(eq(USER_ID), any(ScimPatchRequest.class)))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"Add\", \"path\": \"externalId\", \"value\": \"00u-added\""))
            .exchange()
            .expectStatus()
            .isOk();

        ArgumentCaptor<ScimPatchRequest> scimPatchRequestArgumentCaptor =
            ArgumentCaptor.forClass(ScimPatchRequest.class);

        verify(scimUserFacade).patchUser(eq(USER_ID), scimPatchRequestArgumentCaptor.capture());
        verify(scimUserFacade, never()).deactivateUser(anyLong(), any(ScimPatchRequest.class));

        ScimPatchRequest scimPatchRequest = scimPatchRequestArgumentCaptor.getValue();

        assertThat(scimPatchRequest.operations())
            .containsExactly(new ScimPatchOperation("Add", "externalId", "00u-added"));
    }

    @Test
    void testPatchUserReturnsNotFoundWhenTheFacadeFindsNoUser() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser(USER_ID, "john.doe", "john@example.com", true)));
        when(scimUserFacade.patchUser(eq(USER_ID), any(ScimPatchRequest.class))).thenReturn(Optional.empty());

        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"replace\", \"path\": \"userName\", \"value\": \"jane.doe\""))
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("User 42 not found");
    }

    @Test
    void testPatchUserRejectsAnExternalIdLongerThan255CharactersWithInvalidValue() {
        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody("\"op\": \"Add\", \"path\": \"externalId\", \"value\": \"" + "x".repeat(256) + "\""))
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
            .isEqualTo("externalId must not be longer than 255 characters")
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimUserFacade, userService);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "\"op\": \"move\", \"path\": \"externalId\", \"value\": \"x\" | invalidSyntax",
        "\"path\": \"externalId\", \"value\": \"x\" | invalidSyntax",
        "\"op\": \"add\", \"value\": \"x\" | invalidSyntax",
        "\"op\": \"Remove\" | noTarget",
        "\"op\": \"remove\", \"path\": \"userName\" | mutability",
        "\"op\": \"remove\", \"path\": \"active\" | mutability",
        "\"op\": \"replace\", \"path\": \"active\", \"value\": \"yes\" | invalidValue",
        "\"op\": \"add\", \"path\": \"userName\", \"value\": \"\" | invalidValue"
    })
    void testPatchUserRejectsAnInvalidOperationWithAScimError(String operation, String scimType) {
        webTestClient
            .patch()
            .uri(USER_PATH)
            .contentType(SCIM_JSON)
            .bodyValue(patchBody(operation))
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(SCIM_JSON)
            .expectBody()
            .jsonPath("$.schemas[0]")
            .isEqualTo(ERROR_SCHEMA)
            .jsonPath("$.scimType")
            .isEqualTo(scimType)
            .jsonPath("$.status")
            .isEqualTo("400");

        verifyNoInteractions(scimUserFacade, userService);
    }

    @Test
    void testDeleteUserReturnsNoContent() {
        when(scimUserFacade.deleteUser(USER_ID)).thenReturn(true);

        webTestClient
            .delete()
            .uri(USER_PATH)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(scimUserFacade).deleteUser(USER_ID);
    }

    @Test
    void testDeleteUserReturnsNotFoundWhenTheUserDoesNotExist() {
        when(scimUserFacade.deleteUser(USER_ID)).thenReturn(false);

        webTestClient
            .delete()
            .uri(USER_PATH)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    private ScimPatchRequest captureDeactivationPatchRequest() {
        ArgumentCaptor<ScimPatchRequest> scimPatchRequestArgumentCaptor =
            ArgumentCaptor.forClass(ScimPatchRequest.class);

        verify(scimUserFacade).deactivateUser(eq(USER_ID), scimPatchRequestArgumentCaptor.capture());

        return scimPatchRequestArgumentCaptor.getValue();
    }

    private static Stream<String> unstorableUserBodies() {
        return Stream.of(
            userBody("u".repeat(51), "john@example.com", "John", "Doe", null),
            userBody("john.doe", "e".repeat(180) + "@example.com", "John", "Doe", null),
            userBody("john.doe", "john@example.com", "g".repeat(51), "Doe", null),
            userBody("john.doe", "john@example.com", "John", "f".repeat(51), null));
    }

    private static String userBody(
        String userName, String email, String givenName, String familyName, String externalId) {

        return """
            { \
              "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"], \
              "externalId": %s, \
              "userName": "%s", \
              "name": {"givenName": "%s", "familyName": "%s"}, \
              "emails": [{"value": "%s", "type": "work", "primary": true}], \
              "active": true \
            } \
            """.formatted(
            externalId == null ? "null" : "\"" + externalId + "\"", userName, givenName, familyName, email);
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
