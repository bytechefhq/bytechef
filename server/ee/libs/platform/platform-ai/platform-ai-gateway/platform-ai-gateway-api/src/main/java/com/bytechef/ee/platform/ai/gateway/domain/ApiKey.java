/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ApiKey {

    private static final String REDACTED = "***";

    private final String value;

    private ApiKey(String value) {
        this.value = value;
    }

    public static ApiKey of(String plaintext) {
        Validate.notBlank(plaintext, "api key must not be blank");

        return new ApiKey(plaintext);
    }

    @Nullable
    public static ApiKey ofNullable(@Nullable String plaintext) {
        return plaintext == null || plaintext.isBlank() ? null : new ApiKey(plaintext);
    }

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

    @Override
    public int hashCode() {
        return ApiKey.class.hashCode();
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
