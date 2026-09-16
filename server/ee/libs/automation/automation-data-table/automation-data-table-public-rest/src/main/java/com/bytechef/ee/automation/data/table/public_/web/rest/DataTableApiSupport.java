/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.ee.automation.data.table.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Name resolution and environment lookup shared by the public data table controllers.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class DataTableApiSupport {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]*$");

    private final DataTableService dataTableService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public DataTableApiSupport(DataTableService dataTableService, EnvironmentService environmentService) {
        this.dataTableService = dataTableService;
        this.environmentService = environmentService;
    }

    public long environmentId(@Nullable EnvironmentModel environmentModel) {
        Environment environment = environmentService.getEnvironment(
            environmentModel == null ? null : environmentModel.name());

        return environment.ordinal();
    }

    /**
     * Resolves the public name to the guarded id; an unknown name is the typed not-found the handler maps to 404.
     */
    public long resolveTableId(String name) {
        validateTableName(name);

        return dataTableService.getIdByBaseName(name);
    }

    public static void validateTableName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name)
            .matches() || name.startsWith("dt_")) {

            throw new DataTableException(
                "Invalid table name '" + name + "'", DataTableErrorType.DATA_TABLE_NAME_INVALID);
        }
    }

    public static void validateColumnName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name)
            .matches() || ReservedColumns.isReserved(name)) {

            throw new DataTableException(
                "Invalid column name '" + name + "'", DataTableErrorType.COLUMN_NAME_INVALID);
        }
    }
}
