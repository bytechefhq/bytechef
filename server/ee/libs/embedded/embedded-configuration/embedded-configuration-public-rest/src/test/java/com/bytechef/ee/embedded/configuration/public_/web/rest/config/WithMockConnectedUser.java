/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.METHOD, ElementType.TYPE
})
@WithSecurityContext(factory = WithMockConnectedUserSecurityContextFactory.class)
public @interface WithMockConnectedUser {

    String externalUserId();
}
