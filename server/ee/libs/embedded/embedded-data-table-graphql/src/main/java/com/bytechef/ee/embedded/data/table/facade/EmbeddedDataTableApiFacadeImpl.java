/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.data.table.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.owner.Owner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class EmbeddedDataTableApiFacadeImpl implements EmbeddedDataTableApiFacade {

    private final DataTableService dataTableService;

    public EmbeddedDataTableApiFacadeImpl(DataTableService dataTableService) {
        this.dataTableService = dataTableService;
    }

    /**
     * Filtering by an account answers "what would this account see", not "what does this account own" -- the vendor's
     * unowned tables are shared with every account and so appear under each of them. That is the same rule the runtime
     * applies, deliberately: a console that answered a different question from the one the runtime asks would be the
     * wrong tool for diagnosing why an account can reach a table.
     */
    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<DataTableInfo> getDataTables(long environmentId, @Nullable Long ownerId) {
        return dataTableService.listTables(environmentId, toOwner(ownerId));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void assignDataTableOwner(long dataTableId, @Nullable Long ownerId) {
        dataTableService.assignOwner(dataTableId, ownerId == null ? null : Owner.connectedUser(ownerId));
    }

    private static Optional<Owner> toOwner(@Nullable Long ownerId) {
        return ownerId == null ? Optional.empty() : Optional.of(Owner.connectedUser(ownerId));
    }
}
