/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.security;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
public class RemoteApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    @SuppressFBWarnings("EI")
    public RemoteApiKeyAuthenticationToken(long environmentId, User user) {
        super(environmentId, user);
    }
}
