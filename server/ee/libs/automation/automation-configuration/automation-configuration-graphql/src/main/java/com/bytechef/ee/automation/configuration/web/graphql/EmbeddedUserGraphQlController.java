/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.facade.EmbeddedUserFacade;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.web.graphql.WorkspaceUserGraphQlController.WorkspaceUserInfo;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for embedded membership. Authorization is enforced by {@link EmbeddedUserFacade}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class EmbeddedUserGraphQlController {

    private final EmbeddedUserFacade embeddedUserFacade;
    private final PermissionService permissionService;
    private final UserService userService;

    public EmbeddedUserGraphQlController(
        EmbeddedUserFacade embeddedUserFacade, PermissionService permissionService, UserService userService) {

        this.embeddedUserFacade = embeddedUserFacade;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @QueryMapping
    public List<EmbeddedUserView> embeddedUsers() {
        List<EmbeddedUser> embeddedUsers = embeddedUserFacade.getEmbeddedUsers();

        Set<Long> memberUserIds = embeddedUsers.stream()
            .map(EmbeddedUser::getUserId)
            .collect(Collectors.toSet());

        List<EmbeddedUserView> views = new ArrayList<>(
            embeddedUsers.stream()
                .map(EmbeddedUserView::stored)
                .toList());

        for (User tenantAdmin : userService.getUsersByAuthorityName(AuthorityConstants.ADMIN)) {
            if (!memberUserIds.contains(tenantAdmin.getId())) {
                views.add(EmbeddedUserView.inherited(tenantAdmin.getId()));
            }
        }

        return views;
    }

    @QueryMapping
    public Set<String> myEmbeddedScopes(@Argument @Nullable Environment environment) {
        return permissionService.getMyEmbeddedScopes(environment);
    }

    @MutationMapping
    public EmbeddedUserView addEmbeddedUser(@Argument long userId, @Argument WorkspaceRole role) {
        return EmbeddedUserView.stored(embeddedUserFacade.addEmbeddedUser(userId, role));
    }

    @MutationMapping
    public EmbeddedUserView addEmbeddedUserByEmail(@Argument String email, @Argument WorkspaceRole role) {
        return EmbeddedUserView.stored(embeddedUserFacade.addEmbeddedUserByEmail(email, role));
    }

    @MutationMapping
    public EmbeddedUserView updateEmbeddedUserRole(@Argument long userId, @Argument WorkspaceRole role) {
        return EmbeddedUserView.stored(embeddedUserFacade.updateEmbeddedUserRole(userId, role));
    }

    @MutationMapping
    public boolean removeEmbeddedUser(@Argument long userId) {
        return embeddedUserFacade.removeEmbeddedUser(userId);
    }

    @MutationMapping
    public EmbeddedUserView setEmbeddedUserEnvironmentRole(
        @Argument long userId, @Argument Environment environment, @Argument WorkspaceRole role) {

        return EmbeddedUserView.stored(embeddedUserFacade.setEnvironmentRole(userId, environment, role));
    }

    @MutationMapping
    public boolean removeEmbeddedUserEnvironmentRole(@Argument long userId, @Argument Environment environment) {
        embeddedUserFacade.removeEnvironmentRole(userId, environment);

        return true;
    }

    @SchemaMapping(typeName = "EmbeddedUser", field = "user")
    public WorkspaceUserInfo user(EmbeddedUserView embeddedUserView) {
        return userService.fetchUser(embeddedUserView.userId())
            .map(user -> new WorkspaceUserInfo(user.getEmail(), user.getFirstName(), user.getLastName()))
            .orElse(null);
    }

    public record EmbeddedUserView(
        Long id, long userId, String embeddedRole, Long customRoleId, boolean inherited, String createdDate,
        String environment) {

        static EmbeddedUserView stored(EmbeddedUser embeddedUser) {
            Environment environment = embeddedUser.getEnvironment();
            WorkspaceRole embeddedRole = embeddedUser.getEmbeddedRole();

            return new EmbeddedUserView(
                embeddedUser.getId(), embeddedUser.getUserId(), embeddedRole == null ? null : embeddedRole.name(),
                embeddedUser.getCustomRoleId(), false,
                embeddedUser.getCreatedDate() == null ? null : String.valueOf(embeddedUser.getCreatedDate()),
                environment == null ? null : environment.name());
        }

        static EmbeddedUserView inherited(long userId) {
            return new EmbeddedUserView(null, userId, WorkspaceRole.ADMIN.name(), null, true, null, null);
        }
    }
}
