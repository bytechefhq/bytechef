/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.security;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidator;
import java.util.Set;

/**
 * Validates that a URL points to a public, non-internal host.
 *
 * <p>
 * Delegates to the shared {@link com.bytechef.commons.util.UrlValidator}; retained as the AI-observability-facing entry
 * point and to preserve its callers' {@link IllegalArgumentException} contract. The shared validator guards against
 * SSRF by rejecting loopback, private, link-local, multicast, any-local, CGNAT (100.64.0.0/10) and IPv6 unique local
 * (fc00::/7) addresses, checking every A/AAAA record.
 *
 * <p>
 * <b>DNS rebinding (accepted residual risk):</b> because the JDK {@link java.net.http.HttpClient} does not expose a DNS
 * resolver hook, the connect-time resolution happens after this validator returns. Callers that need hardened
 * guarantees SHOULD re-validate per attempt (webhook delivery already does this on every retry).
 *
 * @version ee
 */
public final class AiObservabilityUrlValidator {

    private AiObservabilityUrlValidator() {
    }

    public static void validateExternalUrl(String url) {
        try {
            UrlValidator.validate(url, Set.of());
        } catch (UrlValidationException urlValidationException) {
            throw new IllegalArgumentException(urlValidationException.getMessage(), urlValidationException);
        }
    }
}
