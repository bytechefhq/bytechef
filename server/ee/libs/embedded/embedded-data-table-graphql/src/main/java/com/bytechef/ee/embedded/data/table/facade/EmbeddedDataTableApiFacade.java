/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.data.table.facade;

import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The embedded console's view of data table ownership. This is the HTTP surface and it owns authorization: every method
 * is gated {@code isTenantAdmin()} on the implementation, never on the controller, because a controller wired to an
 * unguarded facade compiles fine and silently drops the check.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedDataTableApiFacade {

    /**
     * @param environmentId the environment to list
     * @param ownerId       the connected user to filter to, or null for every table in the tenant
     */
    List<DataTableInfo> getDataTables(long environmentId, @Nullable Long ownerId);

    /**
     * @param dataTableId the data table to assign
     * @param ownerId     the connected user to assign it to, or null to return it to the vendor
     */
    void assignDataTableOwner(long dataTableId, @Nullable Long ownerId);
}
