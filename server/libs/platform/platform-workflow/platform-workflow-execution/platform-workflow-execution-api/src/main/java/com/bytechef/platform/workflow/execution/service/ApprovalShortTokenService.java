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

package com.bytechef.platform.workflow.execution.service;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ApprovalShortTokenService {

    /**
     * Deletes every entry whose token has expired.
     */
    void deleteExpired();

    /**
     * Returns the token a short id stands for.
     *
     * @param shortId the short id
     * @return the token, or empty when the id is unknown or its token has expired
     */
    Optional<String> fetchToken(String shortId);

    /**
     * Returns the short id of a token, minting one the first time; the same token always maps to the same short id.
     *
     * @param token      the signed resume token
     * @param expiryDate when the short id expires
     * @return the short id
     */
    String mint(String token, Instant expiryDate);
}
