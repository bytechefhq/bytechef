/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProxyPathValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        // absolute and scheme-relative URLs, userinfo and backslash tricks
        "https://evil.com/x", "//evil.com/x", "/\\evil.com", "/a\\b", "@evil.com/x", "/x@evil.com/", "/a/b@c",
        "/redirect/http://evil.com", "relative/path", "evil.com/x", ":80/x", "\\evil.com",
        // dot segments, literal and encoded
        "/a/../../b", "/%2e%2e/b", "/a/%2E%2E/b", "/..", "/.", "/a/..", "/a/.", "/a/./b", "/a/%2e/b", "/%2E",
        "/a/%2e%2E", "/a/.%2e/b", "/a/%2e./b", "/../admin",
        // encoded separators and delimiters that would change the meaning of the path
        "/a%2fb", "/a%2Fb", "/%2f%2fevil.com", "/a%5cb", "/a%5Cb", "/a%3fb=c", "/a%3Fb", "/a%23b", "/a%40b", "/a%2540b",
        // double encoding
        "/%252e%252e/b", "/a%25", "/a%252fb", "/%25",
        // control characters, raw and encoded
        "/a%0d%0ainjected: x", "/a\nb", "/a\rb", "/a\u0000b", "/a%00b", "/a%0Ab", "/a%09b", "/a%7fb", "/a%1fb", "/a\tb",
        "/a b", "/a\u007fb", "/a\u0085b", "/a%c2%85b", "/a%e2%80%a8b",
        // path parameters hide dot segments from upstreams that drop them before resolving
        "/..;/x", "/x/..;/y", "/x/..;a=b/y", "/.;/x", "/x/%2e%2e;/y", "/a/%2e%2e%3b/b", "/a/..%3b/b", "/a/%2E%3Bx/b",
        "/a/..;", "/a/.;a/b", "/a/%2e%2e%3Bx", "/..%3B", "/a/%2e%3b",
        // encoded Unicode look-alikes of dot, slash, backslash, at and semicolon
        "/a/%ef%bc%8e%ef%bc%8e/b", "/a/%e3%80%82%e3%80%82/b", "/a/%e2%80%a4%e2%80%a4/b", "/a/%ef%bc%8f",
        "/a/%ef%bc%8e%ef%bc%8e%ef%bc%9b/b", "/a/%ef%bc%bc", "/a/%ef%bc%a0", "/a/%ef%bc%8e./b", "/a/%ef%bc%9b",
        // whitespace that trimming upstreams strip before resolving dots
        "/a/..%20", "/a/%20..", "/a/%20..%20/b", "/a/%20.", "/a/.%20", "/a/%e3%80%80..",
        // non-ASCII punctuation and symbols are never allowed, whatever they look like
        "/a/%e2%82%ac", "/a/%e3%80%82", "/a/%e2%80%94",
        // empty inner segments
        "/a//b", "/a//", "/a///b", "/a/b//c",
        // query and fragment belong in ProxyRequest.query, never in the path
        "/a?b=c", "/a#frag", "/?", "/#",
        // invalid and overlong percent sequences
        "/%", "/a%", "/a%z", "/a%zz", "/a%2", "/a%2g", "/%c0%ae%c0%ae/b", "/a%c0%afb", "/a%e0%80%afb", "/%ff", "/a%c3",
        "/a%80",
        // non-ASCII, including look-alikes of slash, dot and at
        "/a∕b", "/․․/b", "/a／b", "/a．b", "/a＠b", "/é", "/a​b", "/a" + (char) 0x2028 + "b", "/．．/b"
    })

    void testValidateRejects(String path) {
        assertThatThrownBy(() -> ProxyPathValidator.validate(path))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_PROXY_PATH_REJECTED.getErrorKey());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/", "/services/data/v60.0/query", "/a/b-c_d.json", "/a/%20b", "/sobjects/Account/001",
            "/files/na%C3%AFve.txt", "/a/",
            "/a/b/", "/.well-known/x", "/a/..b", "/a/b..", "/a/.b", "/a/...", "/a/%2e%2e%2e", "/a/+b", "/a/b:c",
            "/a/b;c=d", "/a/(b)", "/a/'b'", "/a/b,c", "/a/$b", "/a/%C3%A9", "/a/%e6%97%a5%e6%9c%ac", "/a/~tilde",
            "/a/!b*"
        })
    void testValidateAccepts(String path) {
        assertThat(ProxyPathValidator.validate(path)).isEqualTo(path);
    }

    @Test
    void testValidateRejectionDoesNotEchoOverlongOrControlInput() {
        String path = "/a\r\nX-Injected: 1" + "b".repeat(1000) + "@";

        assertThatThrownBy(() -> ProxyPathValidator.validate(path))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> {
                assertThat(exception.getMessage()).hasSizeLessThan(300);
                assertThat(exception.getMessage()).doesNotContain("\r", "\n");
            });
    }

    @Test
    void testValidateNullMessage() {
        assertThatThrownBy(() -> ProxyPathValidator.validate(null))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("rejected");
    }

    @Test
    void testPinsHostAndBasePath() {
        assertThat(ProxyPathValidator.pin("https://acme.my.salesforce.com", "/services/x"))
            .hasToString("https://acme.my.salesforce.com/services/x");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2", "/items"))
            .hasToString("https://api.example.com/v2/items");
        assertThatThrownBy(() -> ProxyPathValidator.pin("https://api.example.com/v2", "/../admin"))
            .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> ProxyPathValidator.pin("https://api.example.com", "/x@evil.com/"))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testPinWithTrailingSlashOnBase() {
        assertThat(ProxyPathValidator.pin("https://api.example.com/", "/items"))
            .hasToString("https://api.example.com/items");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2/", "/items"))
            .hasToString("https://api.example.com/v2/items");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2//", "/items"))
            .hasToString("https://api.example.com/v2/items");
    }

    @Test
    void testPinWithRootPath() {
        assertThat(ProxyPathValidator.pin("https://api.example.com", "/"))
            .hasToString("https://api.example.com/");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2", "/"))
            .hasToString("https://api.example.com/v2/");
    }

    @Test
    void testPinWithExplicitPort() {
        assertThat(ProxyPathValidator.pin("https://api.example.com:8443/v2", "/items"))
            .hasToString("https://api.example.com:8443/v2/items");
        assertThat(ProxyPathValidator.pin("http://localhost:8080", "/health"))
            .hasToString("http://localhost:8080/health");
    }

    @Test
    void testPinKeepsHostAndPortFixedForHostileLookingPaths() {
        for (String path : new String[] {
            "/:80/x", "/:@evil.com/x", "/a:b@evil.com", "//evil.com", "/@evil.com"
        }) {
            try {
                var pinned = ProxyPathValidator.pin("https://api.example.com:8443/v2", path);

                assertThat(pinned.getScheme()).isEqualTo("https");
                assertThat(pinned.getHost()).isEqualTo("api.example.com");
                assertThat(pinned.getPort()).isEqualTo(8443);
                assertThat(pinned.getUserInfo()).isNull();
                assertThat(pinned.getRawPath()).startsWith("/v2/");
            } catch (ConfigurationException configurationException) {
                assertThat(configurationException.getErrorKey())
                    .isEqualTo(AppErrorType.APP_PROXY_PATH_REJECTED.getErrorKey());
            }
        }
    }

    @Test
    void testPinPortLikeFirstSegmentStaysInPath() {
        var pinned = ProxyPathValidator.pin("https://api.example.com", "/:80/x");

        assertThat(pinned.getHost()).isEqualTo("api.example.com");
        assertThat(pinned.getPort()).isEqualTo(-1);
        assertThat(pinned.getRawPath()).isEqualTo("/:80/x");
    }

    @Test
    void testPinDoesNotTreatPrefixStringAsUnderBasePath() {
        // A resolved path that merely starts with the base path string, but not at a segment boundary, is outside it.
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2", "/evil"))
            .hasToString("https://api.example.com/v2/evil");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2", "/evil")
            .getRawPath()).isNotEqualTo("/v2evil");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2", "/v2evil")
            .getRawPath()).isEqualTo("/v2/v2evil");
    }

    @Test
    void testPinRejectsEverythingValidateRejects() {
        for (String path : new String[] {
            "/../v2evil", "/%2e%2e/v2evil", "/a/..", "//evil.com", "https://evil.com", "", "x"
        }) {
            assertThatThrownBy(() -> ProxyPathValidator.pin("https://api.example.com/v2", path))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_PROXY_PATH_REJECTED.getErrorKey());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
            // userinfo, query and fragment on the base are rejected rather than ignored
            "https://user:secret@api.example.com", "https://user@api.example.com/v2", "https://api.example.com?x=1",
            "https://api.example.com/v2?x=1", "https://api.example.com/v2#frag",
            // not an absolute http(s) URL with a server-based host
            "api.example.com", "/v2", "ftp://api.example.com", "file:///etc", "javascript:alert(1)", "https:///v2",
            "https://", "https://api_example.com", "https://api.example.com:notaport", "https://api example.com",
            "mailto:a@b.com"
        })
    void testPinRejectsUnusableBaseUri(String baseUri) {
        assertThatThrownBy(() -> ProxyPathValidator.pin(baseUri, "/items"))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_PROXY_PATH_REJECTED.getErrorKey());
    }

    @Test
    void testPinRejectionDoesNotEchoBaseUriCredentials() {
        assertThatThrownBy(() -> ProxyPathValidator.pin("https://user:topsecret@api.example.com", "/items"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("topsecret"));
    }
}
