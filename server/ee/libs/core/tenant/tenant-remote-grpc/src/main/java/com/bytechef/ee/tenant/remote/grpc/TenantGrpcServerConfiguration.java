/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.remote.grpc;

import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMultiTenant
public class TenantGrpcServerConfiguration {

    @Bean
    TenantRemoteServiceGrpcServer tenantRemoteServiceGrpcServer(TenantService tenantService) {
        return new TenantRemoteServiceGrpcServer(tenantService);
    }
}
