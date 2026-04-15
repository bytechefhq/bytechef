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

import java.util.List;
import java.util.Objects;

/**
 * Structured outcome of a bulk connection-reassignment or mark-pending operation. Mirrors {@link BulkPromoteResultDTO}
 * so UI and operational tooling can present partial success consistently across the two bulk flows.
 *
 * <p>
 * Invariants enforced at construction: {@code failures} is non-null and defensively copied, and
 * {@code failed == failures.size()}. {@code total / updated / skipped / failed} are non-negative and
 * {@code updated + skipped + failed <= total} — unaccounted rows are illegal to construct.
 *
 * @param total    rows considered by the operation (pre-filter size of the candidate set)
 * @param updated  rows whose state was successfully advanced in this call
 * @param skipped  rows in a terminal state (e.g. {@code REVOKED}) that could not legally transition — counted
 *                 separately from {@code failed} so a silent no-op does not look like an error
 * @param failed   rows that threw a real error during the update; equal to {@code failures.size()}
 * @param failures per-row failure entries — empty when {@code failed == 0}
 */
public record BulkReassignResultDTO(
    int total, int updated, int skipped, int failed, List<BulkReassignFailureDTO> failures) {

    public BulkReassignResultDTO {
        Objects.requireNonNull(failures, "failures");

        if (total < 0 || updated < 0 || skipped < 0 || failed < 0) {
            throw new IllegalArgumentException(
                "counts must be non-negative; got total=%d updated=%d skipped=%d failed=%d".formatted(
                    total, updated, skipped, failed));
        }

        if (failed != failures.size()) {
            throw new IllegalArgumentException(
                "failed (%d) must equal failures.size() (%d)".formatted(failed, failures.size()));
        }

        if (updated + skipped + failed > total) {
            throw new IllegalArgumentException(
                "updated (%d) + skipped (%d) + failed (%d) exceeds total (%d)".formatted(
                    updated, skipped, failed, total));
        }

        failures = List.copyOf(failures);
    }

    /**
     * Per-row failure entry. {@code connectionId} is exposed as String to match GraphQL's {@code ID} scalar.
     * {@code errorCode} is a stable identifier the UI can key on for localized rendering; {@code message} is sanitized
     * by the facade so raw JDBC or SQL detail never surfaces to an admin toast.
     *
     * @param connectionId the failing connection's ID (string-serialized to match GraphQL's ID scalar)
     * @param errorCode    stable classification — one of {@code ConnectionErrorType} keys for known failures or
     *                     {@code UNEXPECTED} for anything else; non-blank
     * @param message      human-readable message. For known {@code ConfigurationException}s this is the thrown message
     *                     (already operator-safe); for unknown exceptions the facade substitutes the simple class name
     */
    public record BulkReassignFailureDTO(String connectionId, String errorCode, String message) {

        /** Re-exposed for call-site ergonomics; canonical source is {@link BulkFailureCodes#UNEXPECTED_ERROR_CODE}. */
        public static final String UNEXPECTED_ERROR_CODE = BulkFailureCodes.UNEXPECTED_ERROR_CODE;

        public BulkReassignFailureDTO {
            if (connectionId == null || connectionId.isBlank()) {
                throw new IllegalArgumentException("connectionId must be non-blank");
            }

            if (errorCode == null || errorCode.isBlank()) {
                throw new IllegalArgumentException("errorCode must be non-blank");
            }

            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message must be non-blank");
            }
        }

        public static BulkReassignFailureDTO of(long connectionId, String errorCode, String message) {
            return new BulkReassignFailureDTO(String.valueOf(connectionId), errorCode, message);
        }
    }
}
