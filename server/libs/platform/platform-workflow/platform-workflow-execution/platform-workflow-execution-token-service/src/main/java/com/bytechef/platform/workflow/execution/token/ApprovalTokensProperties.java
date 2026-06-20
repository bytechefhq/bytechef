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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code bytechef.approval.signed-token.*} properties for {@code ApprovalTokensAutoConfiguration}.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.approval.signed-token")
@SuppressFBWarnings("EI")
public class ApprovalTokensProperties {

    /**
     * Base64-encoded HMAC-SHA256 secret. If unset, the signing key is derived from the {@code EncryptionKey} bean. If
     * neither is present, the mint path throws and the verify path accepts only legacy unsigned tokens.
     */
    @Nullable
    private String secret;

    /**
     * When true, only signed tokens are accepted; legacy unsigned inner tokens are rejected. Defaults to {@code false}
     * during the deprecation window so approval emails already sent keep working.
     */
    private boolean required;

    /**
     * Default time-to-live for newly minted tokens. Approval links are long-lived (the job stays suspended), so this
     * defaults to 30 days.
     */
    private Duration defaultTtl = Duration.ofDays(30);

    /**
     * Acceptable clock skew when validating expiry.
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
