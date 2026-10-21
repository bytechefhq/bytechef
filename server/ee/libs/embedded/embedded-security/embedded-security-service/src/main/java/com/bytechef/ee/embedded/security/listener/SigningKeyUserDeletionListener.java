/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.listener;

import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.event.UserDeletingEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Deletes the embedded signing keys a user owns when the user is deleted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class SigningKeyUserDeletionListener {

    private final SigningKeyRepository signingKeyRepository;

    @SuppressFBWarnings("EI")
    public SigningKeyUserDeletionListener(SigningKeyRepository signingKeyRepository) {
        this.signingKeyRepository = signingKeyRepository;
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        signingKeyRepository.deleteAllByUserId(event.userId());
    }
}
