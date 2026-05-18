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

package com.bytechef.file.storage.token;

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Duration;
import java.util.Optional;

/**
 * Mints and verifies HMAC-SHA256 signed, time-bounded tokens for the public {@code /file-entries/{id}/content}
 * endpoint. Token format: {@code v1.<exp>.<payload>.<sig>}.
 *
 * @author Ivica Cardic
 */
public interface FileEntryTokens {

    /**
     * Mints a signed token whose payload identifies {@code fileEntry} and expires after {@code ttl}. Throws
     * {@link IllegalStateException} if no signing secret is configured.
     */
    String toSignedToken(FileEntry fileEntry, Duration ttl);

    /**
     * Mints a signed token using the configured default TTL.
     */
    String toSignedToken(FileEntry fileEntry);

    /**
     * Verifies a signed token. Returns the {@link FileEntry} on success, {@link Optional#empty()} on any failure
     * (malformed, bad signature, expired, or bad payload). The caller MUST NOT distinguish failure modes in HTTP
     * responses — all failures should map to a uniform 404.
     */
    Optional<FileEntry> parseSignedToken(String token);

    /**
     * Mints a signed token if a signing secret is configured, otherwise returns empty. Lets emitters fall back to
     * legacy unsigned ids during the deprecation window without a hard runtime dependency on the secret.
     */
    Optional<String> toSignedTokenIfConfigured(FileEntry fileEntry);

    /**
     * Returns true if the token looks like a v1 signed token (starts with {@code "v1."} and has the expected
     * structure). Cheap parse — does not verify the signature. Lets callers distinguish "is this an attempted signed
     * token" from "is this a legacy unsigned id" before invoking the (more expensive) verifier.
     */
    boolean looksLikeSignedToken(String token);
}
