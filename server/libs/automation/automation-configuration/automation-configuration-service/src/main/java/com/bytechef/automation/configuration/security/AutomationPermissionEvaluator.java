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

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.security.ResourceMembershipDecider.Outcome;
import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * Adapts the automation {@link PermissionService} to Spring Security's {@link PermissionEvaluator} so that
 * authorization is expressed as the standard {@code hasPermission(...)} SpEL built-in. The evaluator is deliberately
 * thin: it routes on {@code targetType} and delegates; all RBAC logic and CE/EE conditioning live in
 * {@link PermissionService}.
 *
 * <p>
 * The one exception is {@link ResourceMembershipResolver}: a principal that resolver governs — today, an embedded
 * connected user — is answered from its own membership here, ahead of both the skip check and
 * {@link PermissionService}. It has to happen here rather than inside {@link PermissionService} because a skipped check
 * short-circuits before the service is ever reached. See {@link ResourceMembershipDecider} for the precedence rule.
 *
 * @author Ivica Cardic
 */
public class AutomationPermissionEvaluator implements PermissionEvaluator {

    private final PermissionService permissionService;
    private final ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;

    @SuppressFBWarnings("EI")
    public AutomationPermissionEvaluator(
        PermissionService permissionService,
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider) {

        this.permissionService = permissionService;
        this.resourceMembershipResolverProvider = resourceMembershipResolverProvider;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // Creating a project deployment is a promotion: the role that matters is the one the caller holds in the
        // environment being deployed INTO, which only the deployment itself knows. Were the source environment
        // checked instead, "editor in Development, viewer in Production" would be decorative -- anyone who could edit
        // in Development could put code into Production. The environment is read off the object rather than
        // EnvironmentContext, which holds the source environment at promotion time.
        if (targetDomainObject instanceof ProjectDeploymentDTO projectDeploymentDTO) {
            // Ticket 1051: a governed principal is answered from its own membership, ahead of the skip check below
            // -- that check is exactly why the resolver must be consulted HERE rather than inside PermissionService,
            // since a governed principal would otherwise short-circuit before permissionService is ever reached. An
            // embedded connected user reaching this branch is answered by the resolver's Project predicate, which is
            // what an earlier revision of this comment demanded before the skip mode's classification of this gate
            // could be re-litigated. That classification is now moot: the restricted skip mode it argued about is
            // gone, and the skip below is full-skip only, which no governed principal ever reaches.
            Outcome outcome = ResourceMembershipDecider.decide(
                resourceMembershipResolverProvider, projectDeploymentDTO.projectId(), "Project",
                String.valueOf(permission));

            if (outcome != Outcome.NOT_GOVERNED) {
                return outcome == Outcome.GRANT;
            }

            if (AutomationAuthorizationContext.isSkipChecks()) {
                return true;
            }

            return permissionService.hasWorkspaceScopeForProject(
                projectDeploymentDTO.projectId(), String.valueOf(permission), projectDeploymentDTO.environment());
        }

        // Every other use of the two-argument hasPermission(target, permission) form carries no resource type; fail
        // closed rather than guessing one. No skip check here on purpose -- an unrecognised target is denied in every
        // mode, full skip included.
        return false;
    }

    @Override
    public boolean hasPermission(
        Authentication authentication, Serializable targetId, String targetType, Object permission) {

        // Ticket 1051: a governed principal is answered from its own membership, ahead of the skip check below.
        Outcome outcome = ResourceMembershipDecider.decide(
            resourceMembershipResolverProvider, targetId, targetType, String.valueOf(permission));

        if (outcome != Outcome.NOT_GOVERNED) {
            return outcome == Outcome.GRANT;
        }

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasResourceScope(targetId, targetType, String.valueOf(permission));
    }
}
