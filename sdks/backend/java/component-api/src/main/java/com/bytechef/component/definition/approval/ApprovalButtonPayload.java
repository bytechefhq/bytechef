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

package com.bytechef.component.definition.approval;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record ApprovalButtonPayload(String shortId, boolean approved) {

    private static final String APPROVE_SUFFIX = ":a";
    private static final String DISCARD_SUFFIX = ":d";

    public ApprovalButtonPayload {
        Objects.requireNonNull(shortId, "shortId");

        if (shortId.isEmpty()) {
            throw new IllegalArgumentException("shortId must not be empty");
        }
    }

    public static String encode(String shortId, boolean approved) {
        ApprovalButtonPayload approvalButtonPayload = new ApprovalButtonPayload(shortId, approved);

        return approvalButtonPayload.encoded();
    }

    public static Optional<ApprovalButtonPayload> parse(@Nullable String payload) {
        if (payload == null || payload.length() <= APPROVE_SUFFIX.length()) {
            return Optional.empty();
        }

        String shortId = payload.substring(0, payload.length() - APPROVE_SUFFIX.length());

        if (payload.endsWith(APPROVE_SUFFIX)) {
            return Optional.of(new ApprovalButtonPayload(shortId, true));
        }

        if (payload.endsWith(DISCARD_SUFFIX)) {
            return Optional.of(new ApprovalButtonPayload(shortId, false));
        }

        return Optional.empty();
    }

    public String encoded() {
        return shortId + (approved ? APPROVE_SUFFIX : DISCARD_SUFFIX);
    }
}
