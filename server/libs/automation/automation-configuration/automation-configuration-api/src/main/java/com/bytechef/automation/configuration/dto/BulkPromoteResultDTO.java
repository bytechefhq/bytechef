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
 * Result of a bulk visibility-change operation. Carries a per-row outcome so the caller can surface partial failures
 * instead of bailing on the first error.
 *
 * <p>
 * Invariants enforced at construction: {@code failures} is non-null and is defensively copied into an immutable list;
 * {@code failed == failures.size()}; all counts are non-negative; and {@code promoted + skipped + failed == attempted}
 * — if the loop mis-accounts (drops a row, double-counts, etc.) the record throws {@link IllegalArgumentException}
 * rather than silently producing a nonsense aggregate. Mirrors the stricter cross-check that
 * {@link BulkReassignResultDTO} already enforces, so the admin UI cannot render "promoted 2 of 5" while the DB actually
 * holds 5 rows unchanged.
 *
 * @param attempted number of candidate rows the operation iterated over (pre-filter size)
 * @param promoted  rows whose visibility was successfully advanced
 * @param skipped   rows already at the target visibility when the per-row call ran — a benign concurrent race surfaced
 *                  as {@code CONNECTION_ALREADY_AT_TARGET_VISIBILITY} and intentionally <b>not</b> classified as a
 *                  failure, so the UI can report "N promoted, M skipped" without alarming admins
 * @param failed    rows that threw a real error; equal to {@code failures.size()}
 * @param failures  per-row failure entries — empty when {@code failed == 0}
 */
public record BulkPromoteResultDTO(
    int attempted, int promoted, int skipped, int failed, List<BulkPromoteFailureDTO> failures) {

    public BulkPromoteResultDTO {
        Objects.requireNonNull(failures, "failures");

        if (attempted < 0 || promoted < 0 || skipped < 0 || failed < 0) {
            throw new IllegalArgumentException(
                "counts must be non-negative; got attempted=%d promoted=%d skipped=%d failed=%d".formatted(
                    attempted, promoted, skipped, failed));
        }

        if (failed != failures.size()) {
            throw new IllegalArgumentException(
                "failed (%d) must equal failures.size() (%d)".formatted(failed, failures.size()));
        }

        if (promoted + skipped + failed != attempted) {
            throw new IllegalArgumentException(
                "promoted (%d) + skipped (%d) + failed (%d) must equal attempted (%d)".formatted(
                    promoted, skipped, failed, attempted));
        }

        failures = List.copyOf(failures);
    }

    /**
     * Per-row failure entry. {@code connectionId} is exposed as String to match GraphQL's {@code ID} scalar (avoids
     * implicit Long-to-String coercion per response). {@code errorCode} is a stable identifier the UI can key on for
     * localized rendering; {@code message} is a human-readable fallback that is always populated — sanitized by the
     * facade so raw JDBC or SQL detail never leaks into an admin toast.
     *
     * @param connectionId the failing connection's ID (string-serialized to match GraphQL's ID scalar)
     * @param errorCode    stable classification — one of {@code ConnectionErrorType} keys for known failures or
     *                     {@link BulkFailureCodes#UNEXPECTED_ERROR_CODE} for anything else; non-blank
     * @param message      human-readable message. For known {@code ConfigurationException}s this is the thrown message
     *                     (already operator-safe); for unknown exceptions the facade substitutes the simple class name
     *                     so SQL state / stack detail never surfaces to the client
     */
    public record BulkPromoteFailureDTO(String connectionId, String errorCode, String message) {

        /** Re-exposed for call-site ergonomics; canonical source is {@link BulkFailureCodes#UNEXPECTED_ERROR_CODE}. */
        public static final String UNEXPECTED_ERROR_CODE = BulkFailureCodes.UNEXPECTED_ERROR_CODE;

        public BulkPromoteFailureDTO {
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

        public static BulkPromoteFailureDTO of(long connectionId, String errorCode, String message) {
            return new BulkPromoteFailureDTO(String.valueOf(connectionId), errorCode, message);
        }
    }
}
