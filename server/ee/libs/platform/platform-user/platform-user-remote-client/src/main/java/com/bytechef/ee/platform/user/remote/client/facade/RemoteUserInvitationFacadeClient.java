/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.facade;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.facade.UserInvitationFacade;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteUserInvitationFacadeClient implements UserInvitationFacade {

    @Override
    public User inviteUser(String email, String role) {
        throw new UnsupportedOperationException();
    }
}
