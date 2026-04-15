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

package com.bytechef.automation.configuration.service;

import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * CE resolution: admin bypass, then reach, then ownership.
 *
 * <p>
 * CE writes WORKSPACE for every connection it creates, so in practice every candidate passes on the reach branch. The
 * PRIVATE branch is not dead code: it covers embedded rows, which are force-written PRIVATE, and any row that predates
 * the default flip. Keeping it means the CE and EE resolvers answer identically for every input CE can actually
 * produce, differing only in EE's additional grant lookup.
 *
 * <p>
 * CE has no grants — {@code resource_grant} is an EE artefact — so step 4 of the resolution order is simply absent
 * rather than stubbed.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnCEVersion
public class DefaultResourceVisibilityResolver implements ResourceVisibilityResolver {

    @Override
    public Set<Long> filterVisibleIds(
        String resourceType, long workspaceId, Collection<VisibilityRecord> candidates) {

        boolean admin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        Set<Long> visibleIds = new LinkedHashSet<>();

        for (VisibilityRecord candidate : candidates) {
            ResourceVisibility visibility = candidate.visibility();

            if (admin || visibility.isAtLeast(ResourceVisibility.WORKSPACE) ||
                Objects.equals(currentUserLogin, candidate.createdBy())) {

                visibleIds.add(candidate.id());
            }
        }

        return visibleIds;
    }
}
