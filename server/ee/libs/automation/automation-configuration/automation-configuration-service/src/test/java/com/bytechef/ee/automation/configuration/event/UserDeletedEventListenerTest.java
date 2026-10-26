/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.user.event.UserDeletedEvent;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class UserDeletedEventListenerTest {

    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);
    private final UserDeletedEventListener userDeletedEventListener = new UserDeletedEventListener(
        workspaceUserService);

    @Test
    void testOnUserDeletedRemovesTheUsersWorkspaceMemberships() {
        userDeletedEventListener.onUserDeleted(new UserDeletedEvent(7L));

        verify(workspaceUserService).deleteUserWorkspaceUsers(7L);
    }
}
