/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppUserAuthenticationException extends BadCredentialsException {

    private final transient AppErrorType errorType;

    public AppUserAuthenticationException(AppErrorType errorType, String message) {
        super(message);

        this.errorType = errorType;
    }

    public AppErrorType getErrorType() {
        return errorType;
    }
}
