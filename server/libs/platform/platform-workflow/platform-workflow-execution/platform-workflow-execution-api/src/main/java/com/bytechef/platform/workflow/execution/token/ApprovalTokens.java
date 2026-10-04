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
 * @author Ivica Cardic
 */
public interface ApprovalTokens {

    String toSignedToken(String innerToken, Duration ttl);

    Optional<String> toSignedTokenIfConfigured(String innerToken);

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
}
