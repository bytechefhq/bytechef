/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteApprovalShortTokenService implements ApprovalShortTokenService {

    @Override
    public void deleteExpired() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> fetchToken(String shortId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String mint(String token, Instant expiryDate) {
        throw new UnsupportedOperationException();
    }
}
