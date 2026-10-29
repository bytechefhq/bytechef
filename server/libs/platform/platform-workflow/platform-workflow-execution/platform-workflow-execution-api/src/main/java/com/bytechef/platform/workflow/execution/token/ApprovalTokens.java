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

import com.bytechef.platform.workflow.execution.JobResumeId;
import java.time.Duration;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ApprovalTokens {

    String toSignedToken(String innerToken, Duration ttl);

    Optional<String> toSignedTokenIfConfigured(String innerToken);

    /**
     * Signs the token with the given time-to-live when a signing secret is configured.
     *
     * @param innerToken the token to sign
     * @param ttl        the time-to-live of the signed token
     * @return the signed token, or empty when no signing secret is configured
     */
    Optional<String> toSignedTokenIfConfigured(String innerToken, Duration ttl);

    Optional<String> parseSignedToken(String token);

    boolean looksLikeSignedToken(String token);

    boolean isSignedTokenRequired();

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

    /**
     * Unwraps a possibly signed resume token and parses the job resume id it carries.
     *
     * @param token the possibly signed resume token
     * @return the job resume id, or empty when the token is invalid
     */
    default Optional<JobResumeId> resolveJobResumeId(String token) {
        Optional<String> innerTokenOptional = resolveInnerToken(token);

        if (innerTokenOptional.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(JobResumeId.parse(innerTokenOptional.get()));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }
}
