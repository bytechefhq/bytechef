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

package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.ApiKey;

/**
 * @author Ivica Cardic
 */
public final class McpApiKeyCredentials implements ApiKey {

    private static final int FINGERPRINT_BYTE_LENGTH = 8;

    private final Environment environment;
    private final String id;
    private final String mcpServerSecretKey;
    private final @Nullable String secretKey;

    public McpApiKeyCredentials(Environment environment, String mcpServerSecretKey, @Nullable String secretKey) {
        this.environment = environment;
        this.id = secretKey == null ? "" : fingerprint(secretKey);
        this.mcpServerSecretKey = mcpServerSecretKey;
        this.secretKey = secretKey;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @Nullable
    public String getSecret() {
        return secretKey;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }

    @Override
    public String toString() {
        return "McpApiKeyCredentials{environment=" + environment + ", id='" + id + "'}";
    }

    private static String fingerprint(String secretKey) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(secretKey.getBytes(StandardCharsets.UTF_8));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(Arrays.copyOf(digest, FINGERPRINT_BYTE_LENGTH));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 is not available", noSuchAlgorithmException);
        }
    }
}
