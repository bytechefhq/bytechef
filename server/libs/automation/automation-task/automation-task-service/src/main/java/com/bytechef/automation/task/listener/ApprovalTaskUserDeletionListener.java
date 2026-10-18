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

package com.bytechef.automation.task.listener;

import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.platform.user.event.UserDeletingEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Unassigns the approval tasks assigned to a user when the user is deleted. The tasks stay, since they belong to the
 * workflows that raised them.
 *
 * @author Ivica Cardic
 */
@Component
public class ApprovalTaskUserDeletionListener {

    private final ApprovalTaskRepository approvalTaskRepository;

    @SuppressFBWarnings("EI")
    public ApprovalTaskUserDeletionListener(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        approvalTaskRepository.clearAssignee(event.userId());
    }
}
