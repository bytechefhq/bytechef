/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a decrypted third-party API key in a dedicated value type so the distinction between "a provider's decrypted
 * secret" and "any old {@code String}" survives past the domain boundary. Prior to this type, {@code getApiKey()}
 * returned a bare {@link String} that looked identical to a baseUrl, a provider name, or a user-facing label — the
 * encrypted/plaintext distinction lived only in the field's declared wrapper and evaporated at every caller.
 *
 * <p>
 * Callers that need the raw secret MUST go through {@link #reveal()}. Grepping the codebase for the string
 * {@code .reveal(} now gives an auditable list of every place a plaintext key leaves the domain model.
 *
 * <p>
 * The {@link #toString()} implementation deliberately returns a redacted placeholder; Jackson serialization is blocked
 * by {@link JsonIgnore} on the accessor. Never log {@link #reveal()} — log the {@link #toString()} instead.
 *
 * @version ee
 */
public final class ApiKey {

    private static final String REDACTED = "***";

    private final String value;

    private ApiKey(String value) {
        this.value = value;
    }

    /**
     * Constructs an {@link ApiKey} from a plaintext string. Rejects null/blank values so a provider can never be
     * persisted with an unusable credential.
     */
    public static ApiKey of(String plaintext) {
        Validate.notBlank(plaintext, "api key must not be blank");

        return new ApiKey(plaintext);
    }

    @Nullable
    public static ApiKey ofNullable(@Nullable String plaintext) {
        return plaintext == null || plaintext.isBlank() ? null : new ApiKey(plaintext);
    }

    /**
     * Returns the plaintext key. Every call site that uses this method is a potential leak path — log review, error
     * strings, exception messages, and any caller that stores the return into a longer-lived field all deserve
     * scrutiny. Never pass the result to a general-purpose logger or to a network call other than the provider's own
     * endpoint.
     */
    @JsonIgnore
    public String reveal() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ApiKey apiKey)) {
            return false;
        }

        return Objects.equals(value, apiKey.value);
    }

    /**
     * Returns a constant hash regardless of the underlying secret, so hashCode-based containers don't disclose the
     * plaintext to an attacker with heap access or that can probe hash collisions.
     */
    @Override
    public int hashCode() {
        return ApiKey.class.hashCode();
    }

    /**
     * Always returns {@value #REDACTED}. Do not change — log lines, toasts, and error messages rely on this behavior to
     * keep plaintext credentials out of observability pipelines.
     */
    @Override
    public String toString() {
        return REDACTED;
    }
}
