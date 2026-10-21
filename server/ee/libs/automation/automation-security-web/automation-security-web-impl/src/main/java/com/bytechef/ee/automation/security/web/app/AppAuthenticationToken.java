/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authentication token of an app request. Before authentication it carries the secret key and the tenant read from it;
 * after authentication it carries the {@link AppPrincipal} and the {@value #ROLE_APP} authority.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    public static final String ROLE_APP = "ROLE_APP";

    private static final long UNKNOWN_ENVIRONMENT_ID = -1L;

    private @Nullable String secretKey;
    private boolean userOptional;
    private @Nullable String userToken;

    /**
     * Pre-authentication token, with a placeholder environment that is not a valid ordinal until the app row is read.
     */
    public AppAuthenticationToken(String secretKey, String tenantId) {
        this(secretKey, tenantId, null, false);
    }

    /**
     * Pre-authentication token that also carries the user token header, and whether the request is one that an instance
     * requiring sign-in still answers without a user.
     */
    public AppAuthenticationToken(
        String secretKey, String tenantId, @Nullable String userToken, boolean userOptional) {

        super(UNKNOWN_ENVIRONMENT_ID, tenantId);

        this.secretKey = secretKey;
        this.userOptional = userOptional;
        this.userToken = userToken;
    }

    public AppAuthenticationToken(AppPrincipal appPrincipal) {
        super(
            appPrincipal.environment()
                .ordinal(),
            appPrincipal.tenantId(), appPrincipal, List.of(new SimpleGrantedAuthority(ROLE_APP)));
    }

    public @Nullable String getSecretKey() {
        return secretKey;
    }

    public @Nullable String getUserToken() {
        return userToken;
    }

    public boolean isUserOptional() {
        return userOptional;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.APP_SECRET_KEY;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }
}
