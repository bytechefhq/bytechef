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

import com.bytechef.platform.user.domain.User;

/**
 * Provisions an account for someone an administrator invited, and mails the claim link on which they set their own
 * password.
 *
 * <p>
 * Shared by both invite paths — the tenant-level invite and the workspace-level one — so an invitation means the same
 * thing whichever surface issues it. Duplicating the sequence would let the two drift, and the parts that must not
 * drift are exactly the security-relevant ones: the password is generated here and disclosed to nobody, and the account
 * is activated so the claim link can be issued at all.
 *
 * @author Ivica Cardic
 */
public interface UserInvitationService {

    /**
     * Provisions an account for {@code email} holding the tenant authority {@code role}, and mails the claim link.
     *
     * <p>
     * Assumes the caller has already established that no account exists for this address — the two invite surfaces
     * differ in what they do when one does (the tenant invite rejects it, the workspace invite reuses it), so that
     * decision stays with them.
     *
     * @param email the invitee's email address
     * @param role  the tenant authority to grant, e.g. {@code ROLE_USER}
     * @return the provisioned user
     */
    User inviteUser(String email, String role);

    /**
     * Tells an existing account holder they were added to a workspace.
     *
     * <p>
     * The counterpart to {@link #inviteUser} for the branch where the address already has an account: that recipient
     * has a password already, so the claim link would be both useless and alarming. Without this they are added
     * silently and discover the workspace only by chance.
     *
     * @param user          the existing account holder
     * @param workspaceName the workspace they were added to, for the message body
     */
    void notifyAddedToWorkspace(User user, String workspaceName);
}
