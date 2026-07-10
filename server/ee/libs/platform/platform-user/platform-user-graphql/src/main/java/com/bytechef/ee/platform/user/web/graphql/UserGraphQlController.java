/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.user.facade.UserManagementFacade;
import com.bytechef.ee.platform.user.facade.UserManagementFacade.UsersWithAuthorities;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing Users.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class UserGraphQlController {

    private final UserManagementFacade userManagementFacade;

    @SuppressFBWarnings("EI")
    public UserGraphQlController(UserManagementFacade userManagementFacade) {
        this.userManagementFacade = userManagementFacade;
    }

    @MutationMapping(name = "deleteUser")
    public Boolean deleteUser(@Argument String login) {
        userManagementFacade.deleteUser(login);

        return true;
    }

    @MutationMapping(name = "inviteUser")
    public Boolean inviteUser(@Argument String email, @Argument String password, @Argument String role) {
        userManagementFacade.inviteUser(email, password, role);

        return true;
    }

    @MutationMapping(name = "updateUser")
    public AdminUserDTO updateUser(@Argument String login, @Argument String role) {
        UserManagementFacade.UserWithAuthorities result = userManagementFacade.updateUserRole(login, role);

        return new AdminUserDTO(result.user(), result.authorities());
    }

    @QueryMapping(name = "user")
    public AdminUserDTO user(@Argument String login) {
        return userManagementFacade.fetchUser(login)
            .map(result -> new AdminUserDTO(result.user(), result.authorities()))
            .orElse(null);
    }

    @QueryMapping(name = "users")
    public AdminUserPage users(@Argument Integer pageNumber, @Argument Integer pageSize) {
        UsersWithAuthorities result = userManagementFacade.getUsers(pageNumber, pageSize);

        Page<User> usersPage = result.users();

        List<AdminUserDTO> content = usersPage.getContent()
            .stream()
            .map(user -> new AdminUserDTO(user, result.authorities()))
            .collect(Collectors.toList());

        return new AdminUserPage(
            content, usersPage.getTotalElements(), usersPage.getTotalPages(), usersPage.getNumber(),
            usersPage.getSize());
    }

    @SuppressFBWarnings("EI")
    public record AdminUserPage(
        List<AdminUserDTO> content, long totalElements, int totalPages, int number, int size) {
    }
}
