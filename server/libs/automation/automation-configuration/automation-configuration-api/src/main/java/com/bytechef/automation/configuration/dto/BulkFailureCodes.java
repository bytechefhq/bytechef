/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.dto;

/**
 * Shared error-code vocabulary for bulk-operation failure entries ({@link BulkPromoteResultDTO.BulkPromoteFailureDTO},
 * {@link BulkReassignResultDTO.BulkReassignFailureDTO}). Colocated so a drift between the two flows — e.g. one renames
 * {@code UNEXPECTED} to {@code UNKNOWN} while the other does not — is a single-file concern, not a cross-file audit.
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
