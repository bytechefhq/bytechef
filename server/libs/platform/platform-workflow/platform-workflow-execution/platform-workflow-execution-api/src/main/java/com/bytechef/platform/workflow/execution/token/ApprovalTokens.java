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

package com.bytechef.platform.workflow.execution.token;

import java.time.Duration;
import java.util.Optional;

/**
 * Mints and verifies HMAC-SHA256 signed, time-bounded wrappers around the opaque capability tokens
 * ({@code JobResumeId}/{@code ApprovalId} Base64 strings) embedded in anonymous approval/resume links. Token format:
 * {@code v1.<exp>.<payload>.<sig>} where {@code <payload>} is the Base64URL rendering of the inner token string.
 *
 * <p>
 * The wrapped inner token stays unchanged (it is also used as a persisted value), so signing happens only at the URL
 * emit/consume boundary. Without a signature an attacker can decode and forge an inner token (retarget the job id, or
 * flip {@code approved}); the HMAC binds the whole inner string so it can no longer be tampered with.
 *
 * @author Ivica Cardic
 */
public interface ApprovalTokens {

    /**
     * Mints a signed token wrapping {@code innerToken} that expires after {@code ttl}. Throws
     * {@link IllegalStateException} if no signing secret is configured.
     */
    String toSignedToken(String innerToken, Duration ttl);

    /**
     * Mints a signed token using the configured default TTL.
     */
    String toSignedToken(String innerToken);

    /**
     * Mints a signed token if a signing secret is configured, otherwise returns empty. Lets emitters fall back to the
     * legacy unsigned inner token during the deprecation window without a hard dependency on the secret.
     */
    Optional<String> toSignedTokenIfConfigured(String innerToken);

    /**
     * Verifies a signed token. Returns the unwrapped inner token string on success, {@link Optional#empty()} on any
     * failure (malformed, bad signature, expired, or bad payload). Callers MUST NOT distinguish failure modes in
     * responses.
     */
    Optional<String> parseSignedToken(String token);

    /**
     * Returns true if {@code token} looks like a {@code v1} signed token (cheap structural check, no signature
     * verification). Lets callers tell "attempted signed token" from "legacy unsigned inner token".
     */
    boolean looksLikeSignedToken(String token);

    /**
     * Whether only signed tokens are accepted ({@code bytechef.approval.signed-token.required}). While {@code false},
     * callers may fall back to accepting a legacy unsigned inner token; while {@code true}, a token that is not a valid
     * signed token must be rejected.
     */
    boolean isSignedTokenRequired();

    /**
     * Resolves a request-supplied token to its inner token string, encapsulating the migration policy: a valid signed
     * token is unwrapped; a token that <em>looks</em> signed but fails verification (tampered/expired) is rejected; an
     * unsigned token is accepted as a legacy inner token only while signing is not required. Returns
     * {@link Optional#empty()} when the token must be rejected.
     */
    default Optional<String> resolveInnerToken(String token) {
        Optional<String> verified = parseSignedToken(token);

        if (verified.isPresent()) {
            return verified;
        }

        if (looksLikeSignedToken(token) || isSignedTokenRequired()) {
            return Optional.empty();
        }

        return Optional.ofNullable(token);
    }
}
