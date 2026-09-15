/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserInvitationService;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteUserInvitationServiceClient implements UserInvitationService {

    @Override
    public User inviteUser(String email, String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notifyAddedToWorkspace(User user, String workspaceName) {
        throw new UnsupportedOperationException();
    }
}
