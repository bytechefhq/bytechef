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

package com.bytechef.platform.user.service;

/**
 * SPI seam letting the user modules revoke a departing user's API keys without depending on the security modules.
 * {@code platform-security} already depends on {@code platform-user-api}, so the reverse call inverts through an
 * interface declared here and implemented there — the same shape as {@link WorkspaceMembershipAssigner}.
 *
 * <p>
 * A key authenticates as the user who owns it, so it has to die with the account rather than outlive it. That is not
 * only a security property: {@code api_key.user_id} is NOT NULL under the foreign key {@code fk_api_key_user}, so
 * without this the delete of any user who ever created a key fails on a constraint violation instead of succeeding.
 *
 * @author Ivica Cardic
 */
public interface ApiKeyRevoker {

    /**
     * Deletes every API key the user owns.
     *
     * <p>
     * Called inside the delete's transaction and before the user row goes, so a failure rolls the account back with the
     * keys rather than leaving one without the other. Revoking key by key rather than in bulk keeps each deletion on
     * the audit trail: a credential that disappears without a record is the gap the trail exists to close.
     *
     * @param userId the user being deleted
     */
    void revokeAll(long userId);
}
