/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Sanitizes HTTP headers for the app proxy to strip credential and hop-by-hop headers from requests, and cookies from
 * responses. ByteChef applies the connection's own authorization; an app must not supply its own, spoof proxy metadata,
 * or inject headers.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ProxyHeaders {

    private static final Set<String> FORBIDDEN_REQUEST = Set.of(
        "authorization", "proxy-authorization", "cookie", "host", "content-length", "connection", "expect",
        "keep-alive",
        "proxy-authenticate", "te", "trailer", "transfer-encoding", "upgrade",
        "x-real-ip", "x-original-url", "forwarded");

    private static final Set<String> FORBIDDEN_RESPONSE = Set.of(
        "set-cookie", "set-cookie2", "connection", "keep-alive", "transfer-encoding", "trailer", "upgrade");

    private ProxyHeaders() {
    }

    /**
     * Sanitizes request headers by removing forbidden headers and headers with CRLF injection.
     *
     * @param headers a nullable map of header names to lists of values
     * @return a new LinkedHashMap with sanitized headers, preserving original casing and order
     */
    public static Map<String, List<String>> sanitizeRequest(@Nullable Map<String, List<String>> headers) {
        if (headers == null) {
            return new LinkedHashMap<>();
        }

        Map<String, List<String>> sanitized = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();

            if (name == null || containsHeaderInjection(name)) {
                continue;
            }

            String lowerName = name.toLowerCase(Locale.ROOT);

            if (FORBIDDEN_REQUEST.contains(lowerName)) {
                continue;
            }

            if (lowerName.startsWith("x-forwarded-")) {
                continue;
            }

            if (values != null && values.stream()
                .anyMatch(ProxyHeaders::containsHeaderInjection)) {
                continue;
            }

            if (values == null) {
                continue;
            }

            sanitized.put(name, values);
        }

        return sanitized;
    }

    /**
     * Sanitizes response headers by removing cookies and hop-by-hop headers.
     *
     * @param headers a nullable map of header names to lists of values
     * @return a new LinkedHashMap with sanitized headers, preserving original casing and order
     */
    public static Map<String, List<String>> sanitizeResponse(@Nullable Map<String, List<String>> headers) {
        if (headers == null) {
            return new LinkedHashMap<>();
        }

        Map<String, List<String>> sanitized = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();

            if (name == null || containsHeaderInjection(name)) {
                continue;
            }

            if (FORBIDDEN_RESPONSE.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (values != null && values.stream()
                .anyMatch(ProxyHeaders::containsHeaderInjection)) {
                continue;
            }

            if (values == null) {
                continue;
            }

            sanitized.put(name, values);
        }

        return sanitized;
    }

    /**
     * Checks if a string contains carriage return (CR, \r) or line feed (LF, \n) characters.
     *
     * @param value the string to check
     * @return true if the value contains CR or LF, false otherwise
     */
    private static boolean containsHeaderInjection(String value) {
        if (value == null) {
            return false;
        }
        return value.contains("\r") || value.contains("\n");
    }
}
