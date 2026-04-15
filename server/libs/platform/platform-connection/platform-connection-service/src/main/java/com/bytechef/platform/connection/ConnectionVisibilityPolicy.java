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

package com.bytechef.platform.connection;

import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Connections support every rung: a credential legitimately serves one person, one workspace, or every workspace in the
 * organization. They are the only resource type for which ORGANIZATION is meaningful — a resource that belongs to one
 * workspace has no representation outside it.
 *
 * <p>
 * Created WORKSPACE. Sharing a connection conveys use and existence, not the secret: the REST boundary obfuscates
 * authorization parameters and no facade mutates them after creation.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionVisibilityPolicy implements ResourceVisibilityPolicy {

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceVisibility defaultVisibility() {
        return ResourceVisibility.WORKSPACE;
    }

    @Override
    public Set<ResourceVisibility> supportedVisibilities() {
        return Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE, ResourceVisibility.ORGANIZATION);
    }
}
