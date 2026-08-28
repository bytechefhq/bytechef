/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.data.table.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.data.table.facade.EmbeddedDataTableApiFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * The embedded console's data table ownership surface. Authorization lives on {@link EmbeddedDataTableApiFacade}, not
 * here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class EmbeddedDataTableGraphQlController {

    private final EmbeddedDataTableApiFacade embeddedDataTableApiFacade;
    private final EnvironmentService environmentService;

    public EmbeddedDataTableGraphQlController(
        EmbeddedDataTableApiFacade embeddedDataTableApiFacade, EnvironmentService environmentService) {

        this.embeddedDataTableApiFacade = embeddedDataTableApiFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<EmbeddedDataTable> embeddedDataTables(
        @Argument Long environmentId, @Argument @Nullable Long ownerId) {

        Environment environment = environmentService.getEnvironment(environmentId);

        List<DataTableInfo> dataTableInfos = embeddedDataTableApiFacade.getDataTables(environment.ordinal(), ownerId);

        return dataTableInfos.stream()
            .map(
                dataTableInfo -> new EmbeddedDataTable(
                    dataTableInfo.id(), dataTableInfo.baseName(), dataTableInfo.description(),
                    dataTableInfo.ownerId()))
            .toList();
    }

    @MutationMapping
    public boolean assignEmbeddedDataTableOwner(@Argument AssignDataTableOwnerInput input) {
        embeddedDataTableApiFacade.assignDataTableOwner(input.dataTableId(), input.ownerId());

        return true;
    }

    public record AssignDataTableOwnerInput(Long dataTableId, @Nullable Long ownerId) {
    }

    /**
     * A null ownerId is the vendor's, which the visibility rule shares with every account.
     */
    public record EmbeddedDataTable(Long id, String baseName, String description, @Nullable Long ownerId) {
    }
}
