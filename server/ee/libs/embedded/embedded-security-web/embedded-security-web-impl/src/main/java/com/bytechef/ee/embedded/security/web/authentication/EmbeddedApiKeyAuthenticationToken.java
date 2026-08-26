/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String externalUserId;
    private String secretKey;

    public EmbeddedApiKeyAuthenticationToken(
        long environmentId, String externalUserId, String secretKey, String tenantId) {

        super(environmentId, tenantId);

        this.externalUserId = externalUserId;
        this.secretKey = secretKey;
    }

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyAuthenticationToken(User user) {
        super(user);
    }

    /**
     * The authenticated form, produced by {@code EmbeddedApiKeyAuthenticationProvider} once the pre-authentication
     * token has been checked. Carries the environment forward rather than dropping it: this is the token that ends up
     * in the {@code SecurityContext} (see {@code ApiKeyAuthenticationFilter}, which stores the provider's result, not
     * the converter's), so anything downstream asking "which environment is this caller in" has only this to ask.
     * Ticket 1051's {@code ConnectedUserResourceMembershipResolver} is the first such caller.
     *
     * <p>
     * The external user id is deliberately not carried: it is already the {@code User}'s username, which is what
     * {@code SecurityUtils.fetchCurrentUserLogin()} reads, so a second copy on the token would be written and never
     * read.
     */
    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyAuthenticationToken(long environmentId, User user) {
        super(environmentId, user);
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
