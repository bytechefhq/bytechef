/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.exception.ScimBadRequestException;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 */
class ScimUserFacadeTest {

    private static final long USER_ID = 42L;

    private final UserService userService = mock(UserService.class);
    private final ScimUserFacade scimUserFacade = new ScimUserFacade(userService);

    @Test
    void testIsDeactivationForScimUserWhenActiveUserBecomesInactive() {
        assertThat(ScimUserFacade.isDeactivation(createUser(true), createScimUser(false))).isTrue();
    }

    @Test
    void testIsDeactivationForScimUserWhenActiveUserStaysActive() {
        assertThat(ScimUserFacade.isDeactivation(createUser(true), createScimUser(true))).isFalse();
    }

    @Test
    void testIsDeactivationForScimUserWhenUserIsAlreadyInactive() {
        assertThat(ScimUserFacade.isDeactivation(createUser(false), createScimUser(false))).isFalse();
        assertThat(ScimUserFacade.isDeactivation(createUser(false), createScimUser(true))).isFalse();
    }

    @Test
    void testIsDeactivationForPatchWithActivePathAndStringValue() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(new ScimPatchOperation("replace", "active", "false"));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest)).isTrue();
    }

    @Test
    void testIsDeactivationForPatchWithActivePathAndBooleanValue() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("Replace", "active", Boolean.FALSE));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest)).isTrue();
    }

    @Test
    void testIsDeactivationForPatchWithPathlessValueMap() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("replace", null, Map.of("active", false)));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest)).isTrue();
    }

    @Test
    void testIsDeactivationForPatchWithAddOperation() {
        ScimPatchRequest addWithPath = createPatchRequest(new ScimPatchOperation("Add", "active", "false"));
        ScimPatchRequest addWithoutPath = createPatchRequest(
            new ScimPatchOperation("add", null, Map.of("active", false)));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), addWithPath)).isTrue();
        assertThat(ScimUserFacade.isDeactivation(createUser(true), addWithoutPath)).isTrue();
    }

    @Test
    void testIsDeactivationForPatchRejectsRemoveOfActive() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(new ScimPatchOperation("remove", "active", null));

        assertScimBadRequest(
            () -> ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest),
            ScimBadRequestException.MUTABILITY);
    }

    @Test
    void testIsDeactivationForPatchUsesLastActiveOperation() {
        ScimPatchRequest deactivateThenActivate = createPatchRequest(
            new ScimPatchOperation("replace", "active", "false"), new ScimPatchOperation("replace", "active", "true"));
        ScimPatchRequest activateThenDeactivate = createPatchRequest(
            new ScimPatchOperation("replace", "active", "true"), new ScimPatchOperation("replace", "active", "false"));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), deactivateThenActivate)).isFalse();
        assertThat(ScimUserFacade.isDeactivation(createUser(true), activateThenDeactivate)).isTrue();
    }

    @Test
    void testIsDeactivationForPatchWithoutActiveOperation() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("replace", "name.givenName", "John"),
            new ScimPatchOperation("replace", null, Map.of("displayName", "John Doe")));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest)).isFalse();
    }

    @Test
    void testIsDeactivationForPatchWhenUserIsAlreadyInactive() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(new ScimPatchOperation("replace", "active", "false"));

        assertThat(ScimUserFacade.isDeactivation(createUser(false), scimPatchRequest)).isFalse();
    }

    @Test
    void testCreateUserMapsScimAttributes() {
        User createdUser = createUser(true);

        when(userService.create(any(AdminUserDTO.class))).thenReturn(createdUser);

        ScimUser scimUser = new ScimUser(
            List.of(ScimUser.SCHEMA), null, "ext-1", "john.doe", new ScimName("John Doe", "Doe", "John"), "John Doe",
            List.of(
                new ScimEmail("john.home@example.com", "home", false),
                new ScimEmail("john.work@example.com", "work", true)),
            true, null, null);

        User user = scimUserFacade.createUser(scimUser);

        AdminUserDTO adminUserDTO = captureCreatedAdminUserDTO();

        assertThat(user).isSameAs(createdUser);
        assertThat(adminUserDTO.getId()).isNull();
        assertThat(adminUserDTO.getLogin()).isEqualTo("john.doe");
        assertThat(adminUserDTO.getEmail()).isEqualTo("john.work@example.com");
        assertThat(adminUserDTO.getFirstName()).isEqualTo("John");
        assertThat(adminUserDTO.getLastName()).isEqualTo("Doe");
        assertThat(adminUserDTO.getExternalId()).isEqualTo("ext-1");
        assertThat(adminUserDTO.isActivated()).isTrue();
    }

    @Test
    void testCreateUserPassesInactiveFlag() {
        when(userService.create(any(AdminUserDTO.class))).thenReturn(createUser(false));

        scimUserFacade.createUser(createScimUser(false));

        AdminUserDTO adminUserDTO = captureCreatedAdminUserDTO();

        assertThat(adminUserDTO.isActivated()).isFalse();
    }

    @Test
    void testCreateUserWithActiveFalseDeactivatesTheCreatedUser() {
        // UserService.create always activates the user it creates
        User createdUser = createUser(true);

        when(userService.create(any(AdminUserDTO.class))).thenReturn(createdUser);

        User user = scimUserFacade.createUser(createScimUser(false));

        assertThat(user).isSameAs(createdUser);
        assertThat(user.isActivated()).isFalse();
        assertThat(user.getActivationKey()).isNull();

        verify(userService).save(createdUser);
    }

    @Test
    void testCreateUserWithActiveTrueDoesNotSaveAgain() {
        User createdUser = createUser(true);

        when(userService.create(any(AdminUserDTO.class))).thenReturn(createdUser);

        User user = scimUserFacade.createUser(createScimUser(true));

        assertThat(user.isActivated()).isTrue();

        verify(userService, never()).save(any(User.class));
    }

    @Test
    void testCreateUserUsesFirstEmailWhenNoneIsPrimary() {
        when(userService.create(any(AdminUserDTO.class))).thenReturn(createUser(true));

        ScimUser scimUser = new ScimUser(
            null, null, null, "john.doe", null, null,
            List.of(
                new ScimEmail("first@example.com", "work", false),
                new ScimEmail("second@example.com", "home", false)),
            true, null, null);

        scimUserFacade.createUser(scimUser);

        AdminUserDTO adminUserDTO = captureCreatedAdminUserDTO();

        assertThat(adminUserDTO.getEmail()).isEqualTo("first@example.com");
    }

    @Test
    void testCreateUserFallsBackToUserNameWhenEmailsAreMissing() {
        when(userService.create(any(AdminUserDTO.class))).thenReturn(createUser(true));

        scimUserFacade.createUser(
            new ScimUser(null, null, null, "john@example.com", null, null, null, true, null, null));
        scimUserFacade.createUser(
            new ScimUser(null, null, null, "jane@example.com", null, null, List.of(), true, null, null));

        ArgumentCaptor<AdminUserDTO> adminUserDTOArgumentCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService, times(2)).create(adminUserDTOArgumentCaptor.capture());

        List<AdminUserDTO> adminUserDTOs = adminUserDTOArgumentCaptor.getAllValues();

        assertThat(adminUserDTOs)
            .extracting(AdminUserDTO::getEmail)
            .containsExactly("john@example.com", "jane@example.com");
    }

    @Test
    void testCreateUserWithoutNameLeavesNamesUnset() {
        when(userService.create(any(AdminUserDTO.class))).thenReturn(createUser(true));

        scimUserFacade.createUser(createScimUser(true));

        AdminUserDTO adminUserDTO = captureCreatedAdminUserDTO();

        assertThat(adminUserDTO.getFirstName()).isNull();
        assertThat(adminUserDTO.getLastName()).isNull();
    }

    @Test
    void testCreateUserRejectsAnExternalIdLongerThan255Characters() {
        ScimUser scimUser = new ScimUser(
            null, null, "x".repeat(256), "john.doe", null, null, null, true, null, null);

        assertScimBadRequest(() -> scimUserFacade.createUser(scimUser), ScimBadRequestException.INVALID_VALUE);

        verify(userService, never()).create(any(AdminUserDTO.class));
    }

    @Test
    void testCreateUserCountsCharactersNotUtf16CodeUnits() {
        when(userService.create(any(AdminUserDTO.class))).thenReturn(createUser(true));

        // 255 characters outside the Basic Multilingual Plane are 510 UTF-16 code units and fit VARCHAR(255)
        String externalId = "\uD83D\uDE00".repeat(255);

        scimUserFacade.createUser(
            new ScimUser(null, null, externalId, "john.doe", null, null, null, true, null, null));

        assertThat(captureCreatedAdminUserDTO().getExternalId()).isEqualTo(externalId);
    }

    @ParameterizedTest
    @MethodSource("tooLongScimUsers")
    void testCreateUserRejectsValuesLongerThanTheirColumns(ScimUser scimUser) {
        assertScimBadRequest(() -> scimUserFacade.createUser(scimUser), ScimBadRequestException.INVALID_VALUE);

        verify(userService, never()).create(any(AdminUserDTO.class));
    }

    @Test
    void testCreateUserRejectsAMissingUserName() {
        ScimUser scimUser = new ScimUser(
            null, null, null, null, null, null, List.of(new ScimEmail("john@example.com", "work", true)), true, null,
            null);

        assertScimBadRequest(() -> scimUserFacade.createUser(scimUser), ScimBadRequestException.INVALID_VALUE);

        verify(userService, never()).create(any(AdminUserDTO.class));
    }

    @Test
    void testReplaceUserRejectsAnExternalIdLongerThan255Characters() {
        ScimUser scimUser = new ScimUser(
            null, null, "x".repeat(256), "john.doe", null, null, null, true, null, null);

        assertScimBadRequest(
            () -> scimUserFacade.replaceUser(USER_ID, scimUser), ScimBadRequestException.INVALID_VALUE);
        assertScimBadRequest(
            () -> scimUserFacade.deactivateUser(USER_ID, scimUser), ScimBadRequestException.INVALID_VALUE);

        verify(userService, never()).fetchUser(USER_ID);
        verify(userService, never()).update(any(AdminUserDTO.class));
    }

    @Test
    void testReplaceUserUpdatesExistingUser() {
        User updatedUser = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(createUser(true)));
        when(userService.update(any(AdminUserDTO.class))).thenReturn(Optional.of(updatedUser));

        ScimUser scimUser = new ScimUser(
            null, null, "ext-2", "new.login", new ScimName(null, "Smith", "Jane"), null,
            List.of(new ScimEmail("jane@example.com", "work", true)), true, null, null);

        Optional<User> userOptional = scimUserFacade.replaceUser(USER_ID, scimUser);

        ArgumentCaptor<AdminUserDTO> adminUserDTOArgumentCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService).update(adminUserDTOArgumentCaptor.capture());

        AdminUserDTO adminUserDTO = adminUserDTOArgumentCaptor.getValue();

        assertThat(userOptional).containsSame(updatedUser);
        assertThat(adminUserDTO.getId()).isEqualTo(USER_ID);
        assertThat(adminUserDTO.getLogin()).isEqualTo("new.login");
        assertThat(adminUserDTO.getEmail()).isEqualTo("jane@example.com");
        assertThat(adminUserDTO.getFirstName()).isEqualTo("Jane");
        assertThat(adminUserDTO.getLastName()).isEqualTo("Smith");
        assertThat(adminUserDTO.getExternalId()).isEqualTo("ext-2");
        assertThat(adminUserDTO.isActivated()).isTrue();
    }

    @Test
    void testReplaceUserKeepsAttributesTheScimResourceDoesNotManage() {
        User existingUser = createUser(true);

        existingUser.setImageUrl("https://example.com/john.png");
        existingUser.setLangKey("de");

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userService.update(any(AdminUserDTO.class))).thenReturn(Optional.of(existingUser));

        scimUserFacade.replaceUser(USER_ID, createScimUser(true));

        AdminUserDTO adminUserDTO = captureUpdatedAdminUserDTO();

        assertThat(adminUserDTO.getImageUrl()).isEqualTo("https://example.com/john.png");
        assertThat(adminUserDTO.getLangKey()).isEqualTo("de");
    }

    @Test
    void testReplaceUserDefaultsMissingLangKey() {
        User existingUser = createUser(true);

        existingUser.setLangKey(null);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userService.update(any(AdminUserDTO.class))).thenReturn(Optional.of(existingUser));

        scimUserFacade.replaceUser(USER_ID, createScimUser(true));

        AdminUserDTO adminUserDTO = captureUpdatedAdminUserDTO();

        assertThat(adminUserDTO.getLangKey()).isEqualTo(UserConstants.DEFAULT_LANGUAGE);
    }

    @Test
    void testDeactivateUserWithScimUserKeepsLangKeyAndImageUrl() {
        User existingUser = createUser(true);

        existingUser.setImageUrl("https://example.com/john.png");
        existingUser.setLangKey("fr");

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userService.update(any(AdminUserDTO.class))).thenReturn(Optional.of(existingUser));

        scimUserFacade.deactivateUser(USER_ID, createScimUser(false));

        AdminUserDTO adminUserDTO = captureUpdatedAdminUserDTO();

        assertThat(adminUserDTO.getImageUrl()).isEqualTo("https://example.com/john.png");
        assertThat(adminUserDTO.getLangKey()).isEqualTo("fr");
        assertThat(adminUserDTO.isActivated()).isFalse();
    }

    @Test
    void testReplaceUserReturnsEmptyWhenUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = scimUserFacade.replaceUser(USER_ID, createScimUser(true));

        assertThat(userOptional).isEmpty();

        verify(userService, never()).update(any(AdminUserDTO.class));
    }

    @Test
    void testDeactivateUserWithScimUserUpdatesActivatedFlag() {
        User updatedUser = createUser(false);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(createUser(true)));
        when(userService.update(any(AdminUserDTO.class))).thenReturn(Optional.of(updatedUser));

        Optional<User> userOptional = scimUserFacade.deactivateUser(USER_ID, createScimUser(false));

        ArgumentCaptor<AdminUserDTO> adminUserDTOArgumentCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService).update(adminUserDTOArgumentCaptor.capture());

        AdminUserDTO adminUserDTO = adminUserDTOArgumentCaptor.getValue();

        assertThat(userOptional).containsSame(updatedUser);
        assertThat(adminUserDTO.getId()).isEqualTo(USER_ID);
        assertThat(adminUserDTO.isActivated()).isFalse();
    }

    @Test
    void testDeactivateUserWithScimUserReturnsEmptyWhenUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        assertThat(scimUserFacade.deactivateUser(USER_ID, createScimUser(false))).isEmpty();

        verify(userService, never()).update(any(AdminUserDTO.class));
    }

    @Test
    void testPatchUserAppliesSupportedReplaceOperations() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("replace", "userName", "jane.smith"),
            new ScimPatchOperation("replace", "name.givenName", "Jane"),
            new ScimPatchOperation("REPLACE", "name.familyName", "Smith"));

        Optional<User> userOptional = scimUserFacade.patchUser(USER_ID, scimPatchRequest);

        assertThat(userOptional).containsSame(user);
        assertThat(user.getLogin()).isEqualTo("jane.smith");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.isActivated()).isTrue();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserReplacesTheExternalId() {
        User user = createUser(true);

        user.setExternalId("old-external-id");

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.patchUser(
            USER_ID, createPatchRequest(new ScimPatchOperation("replace", "externalId", "new-external-id")));

        assertThat(user.getExternalId()).isEqualTo("new-external-id");
        assertThat(user.getEmail()).isEqualTo("john@example.com");

        verify(userService).save(user);
    }

    @Test
    void testPatchUserAppliesEveryAttributeOfAPathlessValueMap() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.patchUser(
            USER_ID,
            createPatchRequest(
                new ScimPatchOperation(
                    "replace", null, Map.of("externalId", "new-external-id", "name.familyName", "Smith"))));

        assertThat(user.getExternalId()).isEqualTo("new-external-id");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.isActivated()).isTrue();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserStoresUserNameLowercased() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.patchUser(
            USER_ID, createPatchRequest(new ScimPatchOperation("replace", "userName", "Jane.Smith@Example.com")));

        assertThat(user.getLogin()).isEqualTo("jane.smith@example.com");

        verify(userService).save(user);
    }

    @Test
    void testPatchUserIgnoresUnsupportedPaths() {
        User user = createUser(true);

        user.setExternalId("ext-1");

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("replace", "displayName", "Ignored"),
            new ScimPatchOperation("add", "title", "Ignored"),
            new ScimPatchOperation("Add", "emails[type eq \"work\"].value", "ignored@example.com"),
            new ScimPatchOperation("remove", "phoneNumbers", null),
            new ScimPatchOperation("replace", null, Map.of("displayName", "Ignored")),
            new ScimPatchOperation("add", null, Map.of("preferredLanguage", "de")));

        scimUserFacade.patchUser(USER_ID, scimPatchRequest);

        assertThat(user.getLogin()).isEqualTo("john.doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getExternalId()).isEqualTo("ext-1");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.isActivated()).isTrue();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserAddWithPathSetsSingleValuedAttributes() {
        User user = createUser(true);

        user.setExternalId("old-external-id");

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        // Azure AD sends capitalized operation names
        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("Add", "externalId", "new-external-id"),
            new ScimPatchOperation("add", "userName", "Jane.Smith"),
            new ScimPatchOperation("ADD", "name.givenName", "Jane"),
            new ScimPatchOperation("Add", "name.familyName", "Smith"));

        Optional<User> userOptional = scimUserFacade.patchUser(USER_ID, scimPatchRequest);

        assertThat(userOptional).containsSame(user);
        assertThat(user.getExternalId()).isEqualTo("new-external-id");
        assertThat(user.getLogin()).isEqualTo("jane.smith");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.isActivated()).isTrue();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserAddWithoutPathAppliesEveryAttributeOfTheValueMap() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        Map<String, Object> value = new LinkedHashMap<>();

        value.put("externalId", "new-external-id");
        value.put("name.familyName", "Smith");
        value.put("name", Map.of("givenName", "Jane"));
        value.put("active", "False");

        scimUserFacade.patchUser(USER_ID, createPatchRequest(new ScimPatchOperation("Add", null, value)));

        assertThat(user.getExternalId()).isEqualTo("new-external-id");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.isActivated()).isFalse();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserAcceptsCaseInsensitiveAndSchemaQualifiedPaths() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("replace", "urn:ietf:params:scim:schemas:core:2.0:User:externalId", "ext-9"),
            new ScimPatchOperation("replace", "NAME.GIVENNAME", "Jane"),
            new ScimPatchOperation("add", "name", Map.of("familyName", "Smith")));

        scimUserFacade.patchUser(USER_ID, scimPatchRequest);

        assertThat(user.getExternalId()).isEqualTo("ext-9");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
    }

    @Test
    void testPatchUserRemoveUnassignsOptionalAttributes() {
        User user = createUser(true);

        user.setExternalId("ext-1");

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("Remove", "externalId", null),
            new ScimPatchOperation("remove", "name.givenName", null));

        Optional<User> userOptional = scimUserFacade.patchUser(USER_ID, scimPatchRequest);

        assertThat(userOptional).containsSame(user);
        assertThat(user.getExternalId()).isNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getLogin()).isEqualTo("john.doe");
        assertThat(user.isActivated()).isTrue();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserRemoveOfNameUnassignsGivenAndFamilyName() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.patchUser(USER_ID, createPatchRequest(new ScimPatchOperation("remove", "name", null)));

        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
    }

    @Test
    void testPatchUserRejectsRemoveOfUserNameWithMutability() {
        assertPatchRejected(
            ScimBadRequestException.MUTABILITY, new ScimPatchOperation("remove", "userName", null));
    }

    @Test
    void testPatchUserRejectsRemoveOfActiveWithMutability() {
        assertPatchRejected(ScimBadRequestException.MUTABILITY, new ScimPatchOperation("Remove", "active", null));
    }

    @Test
    void testPatchUserRejectsRemoveWithoutPathWithNoTarget() {
        assertPatchRejected(
            ScimBadRequestException.NO_TARGET, new ScimPatchOperation("remove", null, Map.of("externalId", "x")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "move", "copy", "test", ""
    })
    void testPatchUserRejectsUnknownOperationsWithInvalidSyntax(String operation) {
        assertPatchRejected(
            ScimBadRequestException.INVALID_SYNTAX, new ScimPatchOperation(operation, "externalId", "x"));
    }

    @Test
    void testPatchUserRejectsOperationWithoutOpWithInvalidSyntax() {
        assertPatchRejected(ScimBadRequestException.INVALID_SYNTAX, new ScimPatchOperation(null, "externalId", "x"));
    }

    @Test
    void testPatchUserRejectsRequestWithoutOperationsWithInvalidSyntax() {
        assertScimBadRequest(
            () -> scimUserFacade.patchUser(USER_ID, createPatchRequest()), ScimBadRequestException.INVALID_SYNTAX);
        assertScimBadRequest(
            () -> scimUserFacade.patchUser(USER_ID, new ScimPatchRequest(null, null)),
            ScimBadRequestException.INVALID_SYNTAX);

        verify(userService, never()).fetchUser(USER_ID);
    }

    @Test
    void testPatchUserRejectsPathlessAddWithoutObjectValueWithInvalidSyntax() {
        assertPatchRejected(ScimBadRequestException.INVALID_SYNTAX, new ScimPatchOperation("add", null, "x"));
    }

    @Test
    void testPatchUserRejectsAnExternalIdLongerThan255Characters() {
        String externalId = "x".repeat(256);

        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("Add", "externalId", externalId));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("replace", "externalId", externalId));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("add", null, Map.of("externalId", externalId)));
    }

    @Test
    void testPatchUserAcceptsAnExternalIdOf255Characters() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        String externalId = "x".repeat(255);

        scimUserFacade.patchUser(USER_ID, createPatchRequest(new ScimPatchOperation("add", "externalId", externalId)));

        assertThat(user.getExternalId()).isEqualTo(externalId);
    }

    @Test
    void testPatchUserRejectsValuesLongerThanTheirColumns() {
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "userName", "u".repeat(51)));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "name.givenName", "g".repeat(51)));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("replace", "name.familyName", "f".repeat(51)));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("replace", "name", Map.of("givenName", "g".repeat(51))));
    }

    @Test
    void testPatchUserRejectsMissingUserNameAndNonBooleanActive() {
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("replace", "userName", " "));
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "userName", null));
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("replace", "active", "yes"));
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "active", null));
    }

    @Test
    void testPatchUserRejectsMultipleValuesForASingleValuedAttribute() {
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "externalId", List.of("a", "b")));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "externalId", Map.of("a", "b")));
    }

    @Test
    void testPatchUserValidatesEveryOperationBeforeApplyingAny() {
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("replace", "name.givenName", "Jane"),
            new ScimPatchOperation("add", "externalId", "x".repeat(256)));
    }

    @Test
    void testPatchUserReturnsEmptyWhenUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = scimUserFacade.patchUser(
            USER_ID, createPatchRequest(new ScimPatchOperation("replace", "userName", "x")));

        assertThat(userOptional).isEmpty();

        verify(userService, never()).save(any(User.class));
    }

    @Test
    void testDeactivateUserWithPatchAppliesActivePath() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        Optional<User> userOptional = scimUserFacade.deactivateUser(
            USER_ID, createPatchRequest(new ScimPatchOperation("replace", "active", "False")));

        assertThat(userOptional).containsSame(user);
        assertThat(user.isActivated()).isFalse();

        verify(userService).save(user);
    }

    @Test
    void testDeactivateUserWithPatchAppliesPathlessValueMap() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.deactivateUser(
            USER_ID, createPatchRequest(new ScimPatchOperation("replace", null, Map.of("active", false))));

        assertThat(user.isActivated()).isFalse();

        verify(userService).save(user);
    }

    @Test
    void testPatchUserCanReactivateUser() {
        User user = createUser(false);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.patchUser(USER_ID, createPatchRequest(new ScimPatchOperation("replace", "active", true)));

        assertThat(user.isActivated()).isTrue();
    }

    @Test
    void testDeleteUserDeletesByLogin() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(createUser(true)));

        assertThat(scimUserFacade.deleteUser(USER_ID)).isTrue();

        verify(userService).delete("john.doe");
    }

    @Test
    void testDeleteUserReturnsFalseWhenUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        assertThat(scimUserFacade.deleteUser(USER_ID)).isFalse();

        verify(userService, never()).delete(anyString());
    }

    private void assertPatchRejected(String scimType, ScimPatchOperation... scimPatchOperations) {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        ScimPatchRequest scimPatchRequest = createPatchRequest(scimPatchOperations);

        assertScimBadRequest(() -> scimUserFacade.patchUser(USER_ID, scimPatchRequest), scimType);
        assertScimBadRequest(() -> scimUserFacade.deactivateUser(USER_ID, scimPatchRequest), scimType);
        assertScimBadRequest(() -> ScimUserFacade.validate(scimPatchRequest), scimType);

        assertThat(user.getExternalId()).isNull();
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getLogin()).isEqualTo("john.doe");
        assertThat(user.isActivated()).isTrue();

        verify(userService, never()).fetchUser(USER_ID);
        verify(userService, never()).save(any(User.class));
    }

    private static void assertScimBadRequest(ThrowingCallable throwingCallable, String scimType) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(ScimBadRequestException.class)
            .hasFieldOrPropertyWithValue("scimType", scimType);
    }

    private static Stream<ScimUser> tooLongScimUsers() {
        List<ScimEmail> emails = List.of(new ScimEmail("john@example.com", "work", true));

        return Stream.of(
            new ScimUser(null, null, null, "u".repeat(51), null, null, emails, true, null, null),
            new ScimUser(
                null, null, null, "john.doe", null, null,
                List.of(new ScimEmail("e".repeat(180) + "@example.com", "work", true)), true, null, null),
            new ScimUser(
                null, null, null, "john.doe", new ScimName(null, null, "g".repeat(51)), null, emails, true, null,
                null),
            new ScimUser(
                null, null, null, "john.doe", new ScimName(null, "f".repeat(51), null), null, emails, true, null,
                null));
    }

    private AdminUserDTO captureUpdatedAdminUserDTO() {
        ArgumentCaptor<AdminUserDTO> adminUserDTOArgumentCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService).update(adminUserDTOArgumentCaptor.capture());

        return adminUserDTOArgumentCaptor.getValue();
    }

    private AdminUserDTO captureCreatedAdminUserDTO() {
        ArgumentCaptor<AdminUserDTO> adminUserDTOArgumentCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService).create(adminUserDTOArgumentCaptor.capture());

        return adminUserDTOArgumentCaptor.getValue();
    }

    private static ScimPatchRequest createPatchRequest(ScimPatchOperation... scimPatchOperations) {
        return new ScimPatchRequest(
            List.of("urn:ietf:params:scim:api:messages:2.0:PatchOp"), Arrays.asList(scimPatchOperations));
    }

    private static ScimUser createScimUser(boolean active) {
        return new ScimUser(
            List.of(ScimUser.SCHEMA), null, null, "john.doe", null, null,
            List.of(new ScimEmail("john@example.com", "work", true)), active, null, null);
    }

    private static User createUser(boolean activated) {
        User user = new User();

        user.setActivated(activated);
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setId(USER_ID);
        user.setLastName("Doe");
        user.setLogin("john.doe");

        return user;
    }
}
