/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceUser;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.automation.configuration.service.WorkspaceUserService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.user.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceFacadeImpl implements WorkspaceFacade {

    private final AuthorityService authorityService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceFacadeImpl(
        AuthorityService authorityService, UserService userService, WorkspaceService workspaceService,
        WorkspaceUserService workspaceUserService) {

        this.authorityService = authorityService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workspace> getUserWorkspaces(long id) {
        List<Authority> authorities = authorityService.getAuthorities();
        User user = userService.getUser(id);
        List<Workspace> workspaces = workspaceService.getWorkspaces();

        List<String> userAuthorityNames = user.getAuthorityIds()
            .stream()
            .map(authorityId -> CollectionUtils.getFirst(
                authorities, authority -> Objects.equals(authority.getId(), authorityId)))
            .map(Authority::getName)
            .toList();

        if (!userAuthorityNames.contains(AuthorityConstants.ADMIN)) {
            List<Long> userWorkspaceIds = workspaceUserService.getUserWorkspaceUsers(id)
                .stream()
                .map(WorkspaceUser::getWorkspaceId)
                .toList();

            workspaces = workspaces.stream()
                .filter(workspace -> userWorkspaceIds.contains(workspace.getId()))
                .toList();
        }

        return workspaces;
    }
}
