/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web.config;

import com.bytechef.ee.platform.licence.web.EeGraphQlFieldRegistry;
import com.bytechef.ee.platform.licence.web.LicenceEnforcementHandlerInterceptor;
import com.bytechef.ee.platform.licence.web.LicenceEnforcementInstrumentation;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.licence.LicenceManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class LicenceWebConfiguration implements WebMvcConfigurer {

    private final LicenceManager licenceManager;

    @SuppressFBWarnings("EI2")
    public LicenceWebConfiguration(LicenceManager licenceManager) {
        this.licenceManager = licenceManager;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LicenceEnforcementHandlerInterceptor(licenceManager))
            .excludePathPatterns("/api/platform/**/licence/**");
    }

    @Bean
    EeGraphQlFieldRegistry eeGraphQlFieldRegistry(ApplicationContext applicationContext) {
        return new EeGraphQlFieldRegistry(applicationContext);
    }

    @Bean
    LicenceEnforcementInstrumentation licenceEnforcementInstrumentation(
        EeGraphQlFieldRegistry eeGraphQlFieldRegistry, LicenceManager licenceManager) {

        return new LicenceEnforcementInstrumentation(eeGraphQlFieldRegistry, licenceManager);
    }
}
