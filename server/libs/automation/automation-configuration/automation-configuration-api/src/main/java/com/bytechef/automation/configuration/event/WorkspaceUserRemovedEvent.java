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

package com.bytechef.automation.configuration.event;

/**
 * Published when a user is removed from a workspace. Listeners use this to trigger cleanup flows such as marking the
 * removed user's owned connections as PENDING_REASSIGNMENT. Construction rejects a malformed event outright — a silent
 * no-op listener triggered by {@code workspaceId == 0} or a blank login would mask the removal from the reassignment
 * flow.
 *
 * @author Ivica Cardic
 */
public record WorkspaceUserRemovedEvent(long workspaceId, String userLogin) {

    public WorkspaceUserRemovedEvent {
        if (workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive; got " + workspaceId);
        }

        if (userLogin == null || userLogin.isBlank()) {
            throw new IllegalArgumentException("userLogin must be non-blank");
        }
    }
}
