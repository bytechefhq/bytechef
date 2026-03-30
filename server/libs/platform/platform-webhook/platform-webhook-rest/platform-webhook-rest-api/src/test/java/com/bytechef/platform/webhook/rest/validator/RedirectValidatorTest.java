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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RedirectValidatorTest {

    private static final String SERVER_HOST = "example.com";

    @Test
    void testNullUrl() {
        assertThat(RedirectValidator.isValidRedirect(null, SERVER_HOST)).isFalse();
    }

    @Test
    void testEmptyUrl() {
        assertThat(RedirectValidator.isValidRedirect("", SERVER_HOST)).isFalse();
    }

    @Test
    void testBlankUrl() {
        assertThat(RedirectValidator.isValidRedirect("   ", SERVER_HOST)).isFalse();
    }

    @Test
    void testRelativePath() {
        assertThat(RedirectValidator.isValidRedirect("/dashboard", SERVER_HOST)).isTrue();
    }

    @Test
    void testRelativePathWithQuery() {
        assertThat(RedirectValidator.isValidRedirect("/page?param=value", SERVER_HOST)).isTrue();
    }

    @Test
    void testRelativePathDeep() {
        assertThat(RedirectValidator.isValidRedirect("/path/to/resource", SERVER_HOST)).isTrue();
    }

    @Test
    void testRelativePathWithoutLeadingSlash() {
        assertThat(RedirectValidator.isValidRedirect("page.html", SERVER_HOST)).isTrue();
    }

    @Test
    void testSameHostRedirect() {
        assertThat(RedirectValidator.isValidRedirect("https://example.com/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testSameHostRedirectHttp() {
        assertThat(RedirectValidator.isValidRedirect("http://example.com/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testSameHostCaseInsensitive() {
        assertThat(RedirectValidator.isValidRedirect("https://EXAMPLE.COM/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testExternalDomainBlocked() {
        assertThat(RedirectValidator.isValidRedirect("https://evil.com/phishing", SERVER_HOST)).isFalse();
    }

    @Test
    void testExternalDomainWithWhitelist() {
        Set<String> allowedDomains = Set.of("trusted.com", "partner.org");

        assertThat(RedirectValidator.isValidRedirect("https://trusted.com/page", SERVER_HOST, allowedDomains)).isTrue();
    }

    @Test
    void testSubdomainOfWhitelistedDomain() {
        Set<String> allowedDomains = Set.of("trusted.com");

        assertThat(
            RedirectValidator.isValidRedirect("https://sub.trusted.com/page", SERVER_HOST, allowedDomains)).isTrue();
    }

    @Test
    void testSimilarDomainNotAllowed() {
        Set<String> allowedDomains = Set.of("trusted.com");

        // "eviltrrusted.com" is not a subdomain of "trusted.com"
        assertThat(
            RedirectValidator.isValidRedirect("https://eviltrusted.com/page", SERVER_HOST, allowedDomains)).isFalse();
    }

    @Test
    void testProtocolRelativeUrlBlocked() {
        // Protocol-relative URLs can be used to bypass validation
        assertThat(RedirectValidator.isValidRedirect("//evil.com/phishing", SERVER_HOST)).isFalse();
    }

    @Test
    void testJavascriptUrlBlocked() {
        assertThat(RedirectValidator.isValidRedirect("javascript:alert('xss')", SERVER_HOST)).isFalse();
    }

    @Test
    void testJavascriptUrlCaseVariations() {
        assertThat(RedirectValidator.isValidRedirect("JAVASCRIPT:alert('xss')", SERVER_HOST)).isFalse();
        assertThat(RedirectValidator.isValidRedirect("Javascript:alert('xss')", SERVER_HOST)).isFalse();
    }

    @Test
    void testDataUrlBlocked() {
        assertThat(RedirectValidator.isValidRedirect("data:text/html,<script>alert('xss')</script>", SERVER_HOST))
            .isFalse();
    }

    @Test
    void testInvalidUrlBlocked() {
        assertThat(RedirectValidator.isValidRedirect("https://[invalid", SERVER_HOST)).isFalse();
    }

    @Test
    void testNullServerHost() {
        // Without server host, only relative paths are allowed
        assertThat(RedirectValidator.isValidRedirect("/relative/path", null)).isTrue();
        assertThat(RedirectValidator.isValidRedirect("https://any.com/page", null)).isFalse();
    }

    @Test
    void testEmptyWhitelist() {
        Set<String> emptyWhitelist = Set.of();

        assertThat(RedirectValidator.isValidRedirect("https://external.com/page", SERVER_HOST, emptyWhitelist))
            .isFalse();
    }

    @Test
    void testSanitizeRedirectUrlValid() {
        String result = RedirectValidator.sanitizeRedirectUrl("/valid/path", SERVER_HOST, null);

        assertThat(result).isEqualTo("/valid/path");
    }

    @Test
    void testSanitizeRedirectUrlInvalid() {
        String result = RedirectValidator.sanitizeRedirectUrl("https://evil.com", SERVER_HOST, null);

        assertThat(result).isNull();
    }

    @Test
    void testOpenRedirectAttackPatterns() {
        // Common open redirect attack patterns
        assertThat(RedirectValidator.isValidRedirect("//evil.com", SERVER_HOST)).isFalse();
        // URL with @ symbol: "example.com" is the actual host (@ separates userinfo from host)
        // So https://evil.com@example.com actually redirects to example.com
        assertThat(RedirectValidator.isValidRedirect("https://evil.com@example.com", SERVER_HOST)).isTrue();
        // URL with @ symbol attempting to redirect to evil.com
        assertThat(RedirectValidator.isValidRedirect("https://example.com@evil.com/", SERVER_HOST)).isFalse();
    }

    @Test
    void testFragmentInUrl() {
        assertThat(RedirectValidator.isValidRedirect("/page#section", SERVER_HOST)).isTrue();
        assertThat(RedirectValidator.isValidRedirect("https://example.com/page#section", SERVER_HOST)).isTrue();
    }

    @Test
    void testUrlWithPort() {
        assertThat(RedirectValidator.isValidRedirect("https://example.com:8080/page", SERVER_HOST)).isTrue();
    }
}
