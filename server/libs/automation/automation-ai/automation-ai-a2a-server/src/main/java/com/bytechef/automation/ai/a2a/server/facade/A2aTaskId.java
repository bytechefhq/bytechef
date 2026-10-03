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

package com.bytechef.automation.ai.a2a.server.facade;

import com.bytechef.commons.util.EncodingUtils;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivica Cardic
 */
record A2aTaskId(long jobId, UUID nonce) {

    private static final String PREFIX = "a2a";
    private static final String SEPARATOR = ":";

    A2aTaskId {
        Objects.requireNonNull(nonce, "nonce");
    }

    static Optional<A2aTaskId> parse(String value) {
        String decoded;

        try {
            decoded = new String(EncodingUtils.urlDecodeBase64FromString(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }

        String[] items = decoded.split(SEPARATOR);

        if (items.length != 3 || !PREFIX.equals(items[0])) {
            return Optional.empty();
        }

        A2aTaskId a2aTaskId;

        try {
            a2aTaskId = new A2aTaskId(Long.parseLong(items[1]), UUID.fromString(items[2]));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }

        if (!value.equals(a2aTaskId.asString())) {
            return Optional.empty();
        }

        return Optional.of(a2aTaskId);
    }

    String asString() {
        String value = PREFIX + SEPARATOR + jobId + SEPARATOR + nonce;

        return EncodingUtils.urlEncodeBase64ToString(value);
    }
}
