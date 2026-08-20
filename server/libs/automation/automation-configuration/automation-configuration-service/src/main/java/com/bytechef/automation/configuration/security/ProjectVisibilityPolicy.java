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

import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Declares the rungs a project supports. {@code ORGANIZATION} is deliberately absent: a project belongs to one
 * workspace and promoting it organization-wide would surface it in other workspaces' lists, which the project model has
 * no other way to express (resource-visibility spec §3.2).
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectVisibilityPolicy implements ResourceVisibilityPolicy {

    /**
     * Reuses {@link ProjectVisibilityFilter#PROJECT} rather than repeating the literal. The registry looks a policy up
     * by this exact string and returns nothing for an unknown one, so two independently spelled copies would let a
     * rename on either side silently miss: projects would lose their policy — and with it their supported rungs and
     * their default visibility — with no error anywhere.
     */
    @Override
    public String resourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public ResourceVisibility defaultVisibility() {
        return ResourceVisibility.WORKSPACE;
    }

    @Override
    public Set<ResourceVisibility> supportedVisibilities() {
        return Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }
}
