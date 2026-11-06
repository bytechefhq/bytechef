/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.security;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import java.util.Set;

/**
 * @version ee
 */
public final class AiObservabilityUrlValidator {

    private AiObservabilityUrlValidator() {
    }

    public static void validateExternalUrl(String url) {
        try {
            UrlValidatorUtils.validate(url, Set.of());
        } catch (UrlValidationException urlValidationException) {
            throw new IllegalArgumentException(urlValidationException.getMessage(), urlValidationException);
        }
    }
}
