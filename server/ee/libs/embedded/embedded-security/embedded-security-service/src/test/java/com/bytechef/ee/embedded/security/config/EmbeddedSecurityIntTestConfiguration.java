/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.config;

import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.embedded.security.facade.SigningKeyFacadeImpl;
import com.bytechef.ee.embedded.security.service.SigningKeyServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@EnableMethodSecurity
@Import({
    CurrentUserResolver.class, PermissionServiceImpl.class, SigningKeyFacadeImpl.class, SigningKeyServiceImpl.class
})
@Configuration
public class EmbeddedSecurityIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EmbeddedSecurityIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
