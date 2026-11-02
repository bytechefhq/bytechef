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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import org.junit.jupiter.api.Test;

/**
 * The identity resolver looks trivial enough to delete, which is precisely the risk: {@code 'Workspace'} is named by
 * the members read, the workspace update and the custom-role assignment picker, and
 * {@code PermissionService.hasResourceScope} denies every non-tenant-admin when no resolver is registered for the
 * token. Removing this class — or mistyping its discriminator — locks those gates for everyone but tenant admins, whose
 * {@code isTenantAdmin()} short-circuit runs before the registry is ever consulted.
 *
 * @author Ivica Cardic
 */
class WorkspaceOwnershipResolverTest {

    private final WorkspaceOwnershipResolver resolver = new WorkspaceOwnershipResolver();

    @Test
    void testResourceTypeMatchesTheTokenTheWorkspaceGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("Workspace");
    }

    @Test
    void testResolveOwnerTreatsTheIdAsTheOwningWorkspace() {
        assertThat(resolver.resolveOwner(42L)).isEqualTo(ResourceOwner.ofWorkspace(42L));
    }

    @Test
    void testResolveOwnerReportsNoOwningUser() {
        // No ownerUserId, deliberately: CE's owner-isolation fallback would otherwise hide every workspace from every
        // member, since nobody is recorded as a workspace's creator here.
        assertThat(
            resolver.resolveOwner(42L)
                .ownerUserId())
                    .isEmpty();
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }
}
