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

import org.springframework.stereotype.Component;

/**
 * Identity resolver for the {@code 'Workspace'} token: the id <em>is</em> the owning workspace. Lets workspace-scoped
 * gates flow through the same {@code hasResourceScope} path as every other resource type. Returns no
 * {@code ownerUserId}, so CE treats workspaces as shared (permissive) rather than owner-isolated.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkspaceOwnershipResolver implements ResourceOwnershipResolver {

    @Override
    public String resourceType() {
        return "Workspace";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return ResourceOwner.ofWorkspace(id);
    }
}
