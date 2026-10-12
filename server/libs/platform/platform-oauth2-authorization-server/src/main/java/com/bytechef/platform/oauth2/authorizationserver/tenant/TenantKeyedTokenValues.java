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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.RandomUtils;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Generates and parses opaque token values (authorization codes, refresh tokens) that carry the issuing tenant.
 *
 * @author Ivica Cardic
 */
public final class TenantKeyedTokenValues {

    private static final String SEPARATOR = ":";
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private TenantKeyedTokenValues() {
    }

    public static String generate(String tenantId) {
        if (!isValidTenantId(tenantId)) {
            throw new IllegalArgumentException("Tenant id " + tenantId + " cannot be carried by a token value");
        }

        String randomPart = EncodingUtils.urlEncodeBase64ToString(RandomUtils.nextBytes(32));

        return EncodingUtils.urlEncodeBase64ToString(tenantId + SEPARATOR + randomPart);
    }

    public static boolean isValidTenantId(@Nullable String tenantId) {
        if (tenantId == null) {
            return false;
        }

        Matcher matcher = TENANT_ID_PATTERN.matcher(tenantId);

        return matcher.matches();
    }

    public static Optional<String> parseTenantId(@Nullable String value) {
        if (value == null || value.isBlank() || value.indexOf('.') >= 0) {
            return Optional.empty();
        }

        try {
            String decoded = new String(EncodingUtils.urlDecodeBase64FromString(value), StandardCharsets.UTF_8);

            int separatorIndex = decoded.indexOf(SEPARATOR);

            if (separatorIndex <= 0) {
                return Optional.empty();
            }

            String tenantId = decoded.substring(0, separatorIndex);

            if (!isValidTenantId(tenantId)) {
                return Optional.empty();
            }

            return Optional.of(tenantId);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }
}
