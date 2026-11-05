/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
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
    void testIsDeactivationForPatchIgnoresNonReplaceOperations() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("add", "active", "false"), new ScimPatchOperation("remove", "active", null));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest)).isFalse();
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
    void testReplaceUserUpdatesExistingUser() {
        User updatedUser = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(createUser(true)));
        when(userService.update(any(AdminUserDTO.class))).thenReturn(Optional.of(updatedUser));

        ScimUser scimUser = new ScimUser(
            null, null, null, "new.login", new ScimName(null, "Smith", "Jane"), null,
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
    void testPatchUserStoresUserNameLowercased() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        scimUserFacade.patchUser(
            USER_ID, createPatchRequest(new ScimPatchOperation("replace", "userName", "Jane.Smith@Example.com")));

        assertThat(user.getLogin()).isEqualTo("jane.smith@example.com");

        verify(userService).save(user);
    }

    @Test
    void testPatchUserIgnoresUnsupportedPathsAndOperations() {
        User user = createUser(true);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        ScimPatchRequest scimPatchRequest = createPatchRequest(
            new ScimPatchOperation("replace", "displayName", "Ignored"),
            new ScimPatchOperation("add", "userName", "ignored.login"),
            new ScimPatchOperation("remove", "name.givenName", null),
            new ScimPatchOperation("replace", null, Map.of("displayName", "Ignored")));

        scimUserFacade.patchUser(USER_ID, scimPatchRequest);

        assertThat(user.getLogin()).isEqualTo("john.doe");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.isActivated()).isTrue();

        verify(userService).save(user);
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
