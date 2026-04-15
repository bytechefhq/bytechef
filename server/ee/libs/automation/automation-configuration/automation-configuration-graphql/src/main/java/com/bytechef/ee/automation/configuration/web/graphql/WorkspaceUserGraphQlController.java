/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for workspace-user management. Authorization is enforced at the service layer
 * ({@code WorkspaceUserService} methods are {@code @PreAuthorize}-annotated) and pinned by
 * {@code PreAuthorizeAnnotationTest}. Do NOT add caching or transform logic to controller methods that could precede
 * the service call without also adding a matching {@code @PreAuthorize} here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class WorkspaceUserGraphQlController {

    private final UserService userService;
    private final WorkspaceUserService workspaceUserService;

    public WorkspaceUserGraphQlController(UserService userService, WorkspaceUserService workspaceUserService) {
        this.userService = userService;
        this.workspaceUserService = workspaceUserService;
    }

    @QueryMapping
    public List<WorkspaceUser> workspaceUsers(@Argument long workspaceId) {
        return workspaceUserService.getWorkspaceWorkspaceUsers(workspaceId);
    }

    @MutationMapping
    public WorkspaceUser addWorkspaceUser(
        @Argument long workspaceId, @Argument long userId, @Argument WorkspaceRole role) {

        return workspaceUserService.addWorkspaceUser(userId, workspaceId, role);
    }

    @MutationMapping
    public WorkspaceUser updateWorkspaceUserRole(
        @Argument long workspaceId, @Argument long userId, @Argument WorkspaceRole role) {

        return workspaceUserService.updateWorkspaceUserRole(userId, workspaceId, role);
    }

    @MutationMapping
    public boolean removeWorkspaceUser(@Argument long workspaceId, @Argument long userId) {
        return workspaceUserService.removeWorkspaceUser(userId, workspaceId);
    }

    @SchemaMapping(typeName = "WorkspaceUser", field = "user")
    public WorkspaceUserInfo user(WorkspaceUser workspaceUser) {
        User user = userService.getUser(workspaceUser.getUserId());

        return new WorkspaceUserInfo(user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public record WorkspaceUserInfo(String email, String firstName, String lastName) {
    }
}
