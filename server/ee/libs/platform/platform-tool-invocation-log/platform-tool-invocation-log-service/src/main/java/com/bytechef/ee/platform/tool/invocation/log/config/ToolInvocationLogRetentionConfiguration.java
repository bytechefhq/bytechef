/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogRetentionJob;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.service.TenantService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
@ConditionalOnBean(TenantService.class)
@ConditionalOnEEVersion
public class ToolInvocationLogRetentionConfiguration {

    @Bean
    ToolInvocationLogRetentionJob toolInvocationLogRetentionJob(
        ApplicationProperties applicationProperties, TenantService tenantService,
        ToolInvocationLogService toolInvocationLogService) {

        return new ToolInvocationLogRetentionJob(applicationProperties, tenantService, toolInvocationLogService);
    }
}
