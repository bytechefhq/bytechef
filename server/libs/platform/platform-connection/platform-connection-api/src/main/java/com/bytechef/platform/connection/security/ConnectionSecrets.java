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

package com.bytechef.platform.connection.security;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Decides who may view or replace a connection's secret values: only the connection's owner, which is the member who
 * created it or the member it was reassigned to. Tenant admins can still use, rename, tag and delete connections they
 * do not own, but not read or replace their secrets. Organization connections are the exception: every tenant admin may
 * view and replace their secrets.
 *
 * @author Ivica Cardic
 */
public final class ConnectionSecrets {

    private ConnectionSecrets() {
    }

    public static boolean canCurrentUserAccessSecrets(
        @Nullable String createdBy, @Nullable ResourceVisibility visibility) {

        if (isCurrentUserCreator(createdBy)) {
            return true;
        }

        return visibility == ResourceVisibility.ORGANIZATION &&
            SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }

    public static boolean isCurrentUserCreator(@Nullable String createdBy) {
        if (createdBy == null) {
            return false;
        }

        Optional<String> currentUserLogin = SecurityUtils.fetchCurrentUserLogin();

        return currentUserLogin.filter(createdBy::equals)
            .isPresent();
    }
}
