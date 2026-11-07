/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.grpc;

import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.data.table.execution.service.DataTableRowServiceImpl")
public class DataTableGrpcServerConfiguration {

    @Bean
    DataTableRemoteServiceGrpcServer dataTableRemoteServiceGrpcServer(
        DataTableService dataTableService, DataTableRowFacade dataTableRowFacade,
        DataTableRowService dataTableRowService, DataTableWebhookService dataTableWebhookService,
        DataTableWorkspaceResolver dataTableWorkspaceResolver, ObjectMapper objectMapper) {

        return new DataTableRemoteServiceGrpcServer(
            dataTableService, dataTableRowFacade, dataTableRowService, dataTableWebhookService,
            dataTableWorkspaceResolver, new DataTableProtoMapper(objectMapper));
    }
}
