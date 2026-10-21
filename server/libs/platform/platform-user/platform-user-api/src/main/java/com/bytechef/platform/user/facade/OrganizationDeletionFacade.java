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

package com.bytechef.platform.user.facade;

import java.time.Instant;

/**
 * Requests the deletion of the current tenant organization.
 *
 * @author Ivica Cardic
 */
public interface OrganizationDeletionFacade {

    /**
     * Marks the current tenant for deletion and stops its activity. The tenant data is purged after the grace period.
     *
     * @param confirmation the admin's email (or login when there is no email), typed to confirm the deletion
     * @param password     the admin's current password; required for local accounts, ignored for accounts that sign in
     *                     through an external identity provider
     * @return the instant after which the tenant data is purged
     */
    Instant requestDeletion(String confirmation, String password);
}
