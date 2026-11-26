/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.config;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.ee.automation.promotion.test.config.PromotionIntTestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.commons.util", "com.bytechef.ee.embedded.configuration", "com.bytechef.encryption",
        "com.bytechef.jackson.config", "com.bytechef.platform.category",
        "com.bytechef.platform.configuration.workflow.diff",
        "com.bytechef.platform.connection",
        "com.bytechef.platform.tag"
    },
    excludeFilters = @Filter(
        type = FilterType.REGEX,
        pattern = "com\\.bytechef\\.ee\\.embedded\\.configuration\\.ownership\\..*"))
@EnableAutoConfiguration
@Import(AutomationMethodSecurityConfiguration.class)
@Configuration
public class EmbeddedConfigurationPromotionIntTestConfiguration {

    @Import(PromotionIntTestConfiguration.class)
    static class SharedPromotionIntTestConfiguration {
    }
}
