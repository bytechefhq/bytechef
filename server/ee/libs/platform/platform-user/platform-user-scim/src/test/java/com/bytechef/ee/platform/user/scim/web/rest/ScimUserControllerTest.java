/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ScimUserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final ScimUserController scimUserController = new ScimUserController(userService);

    @Test
    void testCreateAssignsRoleUserAndRespectsInactive() {
        User createdUser = user(7L, "jane@acme.com", true);

        when(userService.fetchUserByLogin("jane@acme.com")).thenReturn(Optional.empty());
        when(userService.fetchUserByEmail("jane@acme.com")).thenReturn(Optional.empty());
        when(userService.create(any())).thenReturn(createdUser);

        scimUserController.createUser(scimUser("jane@acme.com", false));

        ArgumentCaptor<AdminUserDTO> adminUserDTOCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService).create(adminUserDTOCaptor.capture());
        verify(userService).save(createdUser);

        assertThat(adminUserDTOCaptor.getValue()
            .getAuthorities()).containsExactly("ROLE_USER");
        assertThat(createdUser.isActivated()).isFalse();
    }

    @Test
    void testCreateDuplicateIsUniquenessConflict() {
        when(userService.fetchUserByLogin("jane@acme.com")).thenReturn(Optional.of(user(7L, "jane@acme.com", true)));

        assertThatThrownBy(() -> scimUserController.createUser(scimUser("jane@acme.com", true)))
            .isInstanceOfSatisfying(ScimException.class, scimException -> {
                assertThat(scimException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(scimException.getScimType()).isEqualTo("uniqueness");
            });

        verify(userService, never()).create(any());
    }

    @Test
    void testPatchActiveFalseDeprovisionsThroughTheService() {
        User user = user(7L, "jane@acme.com", true);

        when(userService.fetchUser(7L)).thenReturn(Optional.of(user));

        scimUserController.patchUser(
            7L, new ScimPatchRequest(List.of(), List.of(new ScimPatchOperation("Replace", "active", false))));

        assertThat(user.isActivated()).isFalse();

        verify(userService).save(user);
    }

    @Test
    void testPatchWithoutPathAcceptsAzureStyleValues() {
        User user = user(7L, "jane@acme.com", true);

        when(userService.fetchUser(7L)).thenReturn(Optional.of(user));

        scimUserController.patchUser(
            7L, new ScimPatchRequest(
                List.of(), List.of(new ScimPatchOperation(
                    "replace", null, Map.of("active", "False", "name.givenName", "Janet", "displayName", "Janet D")))));

        assertThat(user.isActivated()).isFalse();
        assertThat(user.getFirstName()).isEqualTo("Janet");
    }

    @Test
    void testPatchWithoutOperationsIsInvalidSyntax() {
        assertThatThrownBy(() -> scimUserController.patchUser(7L, new ScimPatchRequest(List.of(), null)))
            .isInstanceOfSatisfying(
                ScimException.class, scimException -> assertThat(scimException.getScimType())
                    .isEqualTo("invalidSyntax"));
    }

    @Test
    void testPatchUnsupportedAttributeIsInvalidPath() {
        when(userService.fetchUser(7L)).thenReturn(Optional.of(user(7L, "jane@acme.com", true)));

        assertThatThrownBy(
            () -> scimUserController.patchUser(
                7L, new ScimPatchRequest(List.of(), List.of(new ScimPatchOperation("replace", "title", "CEO")))))
                    .isInstanceOfSatisfying(
                        ScimException.class, scimException -> assertThat(scimException.getScimType())
                            .isEqualTo("invalidPath"));
    }

    @Test
    void testPutKeepsAuthoritiesAndLeavesActiveWhenOmitted() {
        User user = user(7L, "jane@acme.com", true);

        user.setAuthorityIds(List.of(1L));

        when(userService.fetchUser(7L)).thenReturn(Optional.of(user));
        when(userService.fetchUserByLogin("jane.doe@acme.com")).thenReturn(Optional.empty());
        when(userService.fetchUserByEmail("jane.doe@acme.com")).thenReturn(Optional.empty());

        scimUserController.replaceUser(7L, scimUser("Jane.Doe@acme.com", null));

        assertThat(user.getLogin()).isEqualTo("jane.doe@acme.com");
        assertThat(user.isActivated()).isTrue();
        assertThat(user.getAuthorityIds()).containsExactly(1L);
    }

    @Test
    void testUserNameFilterIsCaseInsensitive() {
        when(userService.fetchUserByLogin("john@x.com")).thenReturn(Optional.of(user(9L, "john@x.com", true)));

        assertThat(
            scimUserController.listUsers(1, 100, "userName eq \"John@X.com\"")
                .getBody()
                .totalResults()).isEqualTo(1);
    }

    @Test
    void testUnsupportedFilterIsInvalidFilter() {
        assertThatThrownBy(() -> scimUserController.listUsers(1, 100, "title sw \"C\""))
            .isInstanceOfSatisfying(
                ScimException.class, scimException -> assertThat(scimException.getScimType())
                    .isEqualTo("invalidFilter"));
    }

    @Test
    void testGroupsAreReadOnly() {
        ScimGroupController scimGroupController = new ScimGroupController(mock(AuthorityService.class));

        assertThatThrownBy(() -> scimGroupController.deleteGroup(1L))
            .isInstanceOfSatisfying(
                ScimException.class, scimException -> assertThat(scimException.getStatus())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED));
    }

    private static ScimUser scimUser(String userName, Boolean active) {
        return new ScimUser(
            List.of(ScimUser.SCHEMA), null, null, userName, new ScimName("Jane Doe", "Doe", "Jane"), "Jane Doe",
            List.of(new ScimEmail(userName.toLowerCase(), "work", true)), active, List.of(), null);
    }

    private static User user(long id, String login, boolean activated) {
        User user = new User();

        user.setActivated(activated);
        user.setEmail(login);
        user.setId(id);
        user.setLogin(login);

        return user;
    }
}
