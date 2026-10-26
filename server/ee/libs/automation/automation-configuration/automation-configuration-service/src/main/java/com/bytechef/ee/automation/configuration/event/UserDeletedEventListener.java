/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.event.UserDeletedEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class UserDeletedEventListener {

    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public UserDeletedEventListener(WorkspaceUserService workspaceUserService) {
        this.workspaceUserService = workspaceUserService;
    }

    @EventListener
    public void onUserDeleted(UserDeletedEvent userDeletedEvent) {
        workspaceUserService.deleteUserWorkspaceUsers(userDeletedEvent.userId());
    }
}
