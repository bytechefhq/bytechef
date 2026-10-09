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

package com.bytechef.platform.webhook.rest.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RedirectValidatorUtilsTest {

    private static final String SERVER_HOST = "example.com";

    @Test
    void testNullUrl() {
        assertThat(RedirectValidatorUtils.isValidRedirect(null, SERVER_HOST)).isFalse();
    }

    @Test
    void testEmptyUrl() {
        assertThat(RedirectValidatorUtils.isValidRedirect("", SERVER_HOST)).isFalse();
    }

    @Test
    void testBlankUrl() {
        assertThat(RedirectValidatorUtils.isValidRedirect("   ", SERVER_HOST)).isFalse();
    }

    @Test
    void testRelativePath() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/dashboard", SERVER_HOST)).isTrue();
    }

    @Test
    void testRelativePathWithQuery() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/page?param=value", SERVER_HOST)).isTrue();
    }

    @Test
    void testRelativePathDeep() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/path/to/resource", SERVER_HOST)).isTrue();
    }

    @Test
    void testRelativePathWithoutLeadingSlash() {
        assertThat(RedirectValidatorUtils.isValidRedirect("page.html", SERVER_HOST)).isTrue();
    }

    @Test
    void testSameHostRedirect() {
        assertThat(RedirectValidatorUtils.isValidRedirect("https://example.com/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testSameHostRedirectHttp() {
        assertThat(RedirectValidatorUtils.isValidRedirect("http://example.com/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testSameHostCaseInsensitive() {
        assertThat(RedirectValidatorUtils.isValidRedirect("https://EXAMPLE.COM/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testExternalDomainBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("https://evil.com/phishing", SERVER_HOST)).isFalse();
    }

    @Test
    void testExternalDomainWithWhitelist() {
        Set<String> allowedDomains = Set.of("trusted.com", "partner.org");

        assertThat(RedirectValidatorUtils.isValidRedirect("https://trusted.com/page", SERVER_HOST, allowedDomains))
            .isTrue();
    }

    @Test
    void testSubdomainOfWhitelistedDomain() {
        Set<String> allowedDomains = Set.of("trusted.com");

        assertThat(
            RedirectValidatorUtils.isValidRedirect("https://sub.trusted.com/page", SERVER_HOST, allowedDomains))
                .isTrue();
    }

    @Test
    void testSimilarDomainNotAllowed() {
        Set<String> allowedDomains = Set.of("trusted.com");

        assertThat(
            RedirectValidatorUtils.isValidRedirect("https://eviltrusted.com/page", SERVER_HOST, allowedDomains))
                .isFalse();
    }

    @Test
    void testProtocolRelativeUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("//evil.com/phishing", SERVER_HOST)).isFalse();
    }

    @Test
    void testJavascriptUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("javascript:alert('xss')", SERVER_HOST)).isFalse();
    }

    @Test
    void testJavascriptUrlCaseVariations() {
        assertThat(RedirectValidatorUtils.isValidRedirect("JAVASCRIPT:alert('xss')", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("Javascript:alert('xss')", SERVER_HOST)).isFalse();
    }

    @Test
    void testDataUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("data:text/html,<script>alert('xss')</script>", SERVER_HOST))
            .isFalse();
    }

    @Test
    void testInvalidUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("https://[invalid", SERVER_HOST)).isFalse();
    }

    @Test
    void testNullServerHost() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/relative/path", null)).isTrue();
        assertThat(RedirectValidatorUtils.isValidRedirect("https://any.com/page", null)).isFalse();
    }

    @Test
    void testEmptyWhitelist() {
        Set<String> emptyWhitelist = Set.of();

        assertThat(RedirectValidatorUtils.isValidRedirect("https://external.com/page", SERVER_HOST, emptyWhitelist))
            .isFalse();
    }

    @Test
    void testSanitizeRedirectUrlValid() {
        String result = RedirectValidatorUtils.sanitizeRedirectUrl("/valid/path", SERVER_HOST, null);

        assertThat(result).isEqualTo("/valid/path");
    }

    @Test
    void testSanitizeRedirectUrlInvalid() {
        String result = RedirectValidatorUtils.sanitizeRedirectUrl("https://evil.com", SERVER_HOST, null);

        assertThat(result).isNull();
    }

    @Test
    void testOpenRedirectAttackPatterns() {
        assertThat(RedirectValidatorUtils.isValidRedirect("//evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("https://evil.com@example.com", SERVER_HOST)).isTrue();
        assertThat(RedirectValidatorUtils.isValidRedirect("https://example.com@evil.com/", SERVER_HOST)).isFalse();
    }

    @Test
    void testFragmentInUrl() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/page#section", SERVER_HOST)).isTrue();
        assertThat(RedirectValidatorUtils.isValidRedirect("https://example.com/page#section", SERVER_HOST)).isTrue();
    }

    @Test
    void testUrlWithPort() {
        assertThat(RedirectValidatorUtils.isValidRedirect("https://example.com:8080/page", SERVER_HOST)).isTrue();
    }

    @Test
    void testBackslashUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("\\\\evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("/\\evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("\\/evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("https:\\\\evil.com", SERVER_HOST)).isFalse();
    }

    @Test
    void testControlCharacterUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/\t/evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("/\n/evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("/\r/evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("/\u0000/evil.com", SERVER_HOST)).isFalse();
    }

    @Test
    void testLeadingOrTrailingWhitespaceUrlBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect(" //evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("\t//evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("/dashboard ", SERVER_HOST)).isFalse();
    }

    @Test
    void testSchemeWithoutAuthorityBlocked() {
        assertThat(RedirectValidatorUtils.isValidRedirect("https:evil.com", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("http:evil.com/phishing", SERVER_HOST)).isFalse();
        assertThat(RedirectValidatorUtils.isValidRedirect("https:/evil.com", SERVER_HOST)).isFalse();
    }

    @Test
    void testRelativePathWithColonAfterSlashAllowed() {
        assertThat(RedirectValidatorUtils.isValidRedirect("/page:1", SERVER_HOST)).isTrue();
        assertThat(RedirectValidatorUtils.isValidRedirect("page?time=10:00", SERVER_HOST)).isTrue();
    }
}
