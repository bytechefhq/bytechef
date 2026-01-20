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

package com.bytechef.commons.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Utility class for validating redirect URLs to prevent open redirect vulnerabilities. Open redirects can be exploited
 * for phishing attacks by redirecting users to malicious sites.
 *
 * <p>
 * This validator supports:
 * <ul>
 * <li>Relative paths (always allowed)</li>
 * <li>Absolute URLs to the same host (always allowed)</li>
 * <li>Absolute URLs to whitelisted domains (if configured)</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public final class RedirectValidator {

    private RedirectValidator() {
    }

    /**
     * Validates a redirect URL without a whitelist. Only allows relative paths and same-host redirects.
     *
     * @param redirectUrl the URL to validate
     * @param serverHost  the current server's host for same-host comparison
     * @return true if the redirect is safe, false otherwise
     */
    public static boolean isValidRedirect(String redirectUrl, @Nullable String serverHost) {
        return isValidRedirect(redirectUrl, serverHost, null);
    }

    /**
     * Validates a redirect URL against security constraints.
     *
     * @param redirectUrl    the URL to validate
     * @param serverHost     the current server's host for same-host comparison
     * @param allowedDomains optional set of allowed external domains (null means no external domains allowed)
     * @return true if the redirect is safe, false otherwise
     */
    public static boolean isValidRedirect(
        String redirectUrl, @Nullable String serverHost, @Nullable Set<String> allowedDomains) {

        if (redirectUrl == null || redirectUrl.isBlank()) {
            return false;
        }

        // Block protocol-relative URLs (//evil.com) which can bypass validation
        if (redirectUrl.startsWith("//")) {
            return false;
        }

        // Block javascript: and data: URLs which could execute code
        String lowerUrl = redirectUrl.toLowerCase()
            .trim();

        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:")) {
            return false;
        }

        // Relative paths are safe
        if (isRelativePath(redirectUrl)) {
            return true;
        }

        // Parse the URL to check the host
        try {
            URI uri = new URI(redirectUrl);
            String host = uri.getHost();

            if (host == null) {
                // Relative URL without host - safe
                return true;
            }

            // Same-host redirect is always allowed
            if (serverHost != null && host.equalsIgnoreCase(serverHost)) {
                return true;
            }

            // Check against whitelist if provided
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                for (String allowedDomain : allowedDomains) {
                    if (hostMatchesDomain(host, allowedDomain)) {
                        return true;
                    }
                }
            }

            // External domain not in whitelist
            return false;
        } catch (URISyntaxException e) {
            // Invalid URL - reject
            return false;
        }
    }

    /**
     * Checks if a URL is a relative path (no scheme or host).
     */
    private static boolean isRelativePath(String url) {
        // Relative paths start with / but not //
        if (url.startsWith("/") && !url.startsWith("//")) {
            return true;
        }

        // Also check for relative paths without leading slash (e.g., "page.html")
        // but exclude URLs with schemes (http:, https:, etc.)
        return !url.contains("://") && !url.startsWith("//");
    }

    /**
     * Checks if a host matches an allowed domain pattern. Supports:
     * <ul>
     * <li>Exact match: "example.com" matches "example.com"</li>
     * <li>Subdomain match: "example.com" also matches "sub.example.com"</li>
     * </ul>
     */
    private static boolean hostMatchesDomain(String host, String allowedDomain) {
        String lowerHost = host.toLowerCase();
        String lowerDomain = allowedDomain.toLowerCase();

        // Exact match
        if (lowerHost.equals(lowerDomain)) {
            return true;
        }

        // Subdomain match (host ends with .domain)
        return lowerHost.endsWith("." + lowerDomain);
    }

    /**
     * Sanitizes a redirect URL by returning it only if valid, or null if unsafe.
     *
     * @param redirectUrl    the URL to validate
     * @param serverHost     the current server's host
     * @param allowedDomains optional set of allowed external domains
     * @return the URL if valid, null otherwise
     */
    @Nullable
    public static String sanitizeRedirectUrl(
        String redirectUrl, @Nullable String serverHost, @Nullable Set<String> allowedDomains) {

        if (isValidRedirect(redirectUrl, serverHost, allowedDomains)) {
            return redirectUrl;
        }

        return null;
    }
}
