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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code bytechef.file-storage.signed-url.*} properties for use in {@link FileEntryTokensAutoConfiguration}.
 * Defined here to avoid a circular module dependency between {@code file-storage-api} and {@code app-config}.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.file-storage.signed-url")
@SuppressFBWarnings("EI")
public class FileEntryTokensProperties {

    /**
     * Base64-encoded HMAC-SHA256 secret (at least 32 bytes of entropy). If unset and {@link #required} is true, the
     * application fails to start. If unset and {@code required} is false, the mint path throws on call and the verify
     * path accepts only legacy tokens.
     */
    @Nullable
    private String secret;

    /**
     * When true, the endpoint accepts only signed tokens; legacy unsigned IDs are rejected with 404. Defaults to
     * {@code false} during the deprecation window.
     */
    private boolean required;

    /**
     * Default time-to-live for newly minted tokens. ISO-8601 duration.
     */
    private Duration defaultTtl = Duration.ofHours(24);

    /**
     * Acceptable clock skew when validating expiry (both directions).
     */
    private Duration clockSkew = Duration.ofSeconds(60);

    /**
     * Previous secrets retained for key rotation. Verification tries the active {@link #secret} first, then each entry
     * here in order. Signing always uses {@link #secret}.
     */
    private List<String> previousSecrets = new ArrayList<>();

    @Nullable
    public String getSecret() {
        return secret;
    }

    public void setSecret(@Nullable String secret) {
        this.secret = secret;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public List<String> getPreviousSecrets() {
        return previousSecrets;
    }

    public void setPreviousSecrets(List<String> previousSecrets) {
        this.previousSecrets = previousSecrets;
    }
}
