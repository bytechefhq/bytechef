/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.security;

import com.bytechef.platform.security.web.authentication.ExternalIssuerAuthentication;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
public class RemoteExternalIssuerAuthenticationToken extends AbstractAuthenticationToken
    implements ExternalIssuerAuthentication {

    private final User user;

    @SuppressFBWarnings("EI2")
    public RemoteExternalIssuerAuthenticationToken(User user, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);

        this.user = user;

        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    @SuppressFBWarnings("EI")
    public Object getPrincipal() {
        return user;
    }
}
