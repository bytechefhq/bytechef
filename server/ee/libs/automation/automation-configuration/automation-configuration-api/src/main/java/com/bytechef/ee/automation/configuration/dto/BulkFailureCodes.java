/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.dto;

/**
 * Shared error-code vocabulary for bulk-operation failure entries ({@link BulkReassignResultDTO.BulkReassignFailureDTO}
 * today). Kept as a shared vocabulary rather than folded into its single caller so a second bulk flow does not have to
 * reinvent the codes, and so a drift between flows stays a single-file concern.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class BulkFailureCodes {

    /**
     * Sentinel {@code errorCode} for non-{@code ConfigurationException} failures. Paired with a sanitized message
     * (simple class name) so raw SQL/driver detail never surfaces to the client.
     */
    public static final String UNEXPECTED_ERROR_CODE = "UNEXPECTED";

    private BulkFailureCodes() {
    }
}
