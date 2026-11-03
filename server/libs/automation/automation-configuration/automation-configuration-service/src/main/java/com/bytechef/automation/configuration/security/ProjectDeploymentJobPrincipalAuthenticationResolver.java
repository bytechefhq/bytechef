/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Resolves the user who created a project deployment, with that user's own authorities.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentJobPrincipalAuthenticationResolver implements JobPrincipalAuthenticationResolver {

    private final AuthorityService authorityService;
    private final ProjectDeploymentService projectDeploymentService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentJobPrincipalAuthenticationResolver(
        AuthorityService authorityService, ProjectDeploymentService projectDeploymentService,
        UserService userService) {

        this.authorityService = authorityService;
        this.projectDeploymentService = projectDeploymentService;
        this.userService = userService;
    }

    @Override
    public Optional<Authentication> fetchAuthentication(long jobPrincipalId) {
        return projectDeploymentService.fetchProjectDeployment(jobPrincipalId)
            .map(ProjectDeployment::getCreatedBy)
            .flatMap(userService::fetchUserByLogin)
            .filter(User::isActivated)
            .map(this::createAuthentication);
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }

    private Authentication createAuthentication(User user) {
        List<GrantedAuthority> authorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .flatMap(Optional::stream)
            .map(Authority::getName)
            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
            .toList();

        return UsernamePasswordAuthenticationToken.authenticated(user.getLogin(), null, authorities);
    }
}
