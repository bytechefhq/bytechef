/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProxyTypesTest {

    @Test
    void testProxyRequestCopiesItsMapsDefensively() {
        List<String> values = new ArrayList<>(List.of("a"));
        Map<String, List<String>> query = new HashMap<>(Map.of("q", values));

        ProxyRequest proxyRequest = new ProxyRequest("GET", "/x", query, null, null, BodyEncoding.UTF8, null);

        values.add("b");
        query.put("other", List.of("c"));

        assertThat(proxyRequest.query()).isEqualTo(Map.of("q", List.of("a")));
        assertThat(proxyRequest.headers()).isEmpty();
        assertThatThrownBy(() -> proxyRequest.query()
            .put("x", List.of())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testNullListElementsBecomeEmptyStrings() {
        Map<String, List<String>> headers = new HashMap<>();

        headers.put("h", Arrays.asList("a", null));
        headers.put("n", null);

        ProxyRequest proxyRequest = new ProxyRequest("GET", "/x", headers, headers, null, BodyEncoding.UTF8, null);
        ProxyResponse proxyResponse = new ProxyResponse(200, headers, null, BodyEncoding.UTF8);

        assertThat(proxyRequest.headers()).containsEntry("h", List.of("a", ""))
            .containsEntry("n", List.of());
        assertThat(proxyRequest.query()).containsEntry("h", List.of("a", ""));
        assertThat(proxyResponse.headers()).containsEntry("h", List.of("a", ""));
    }

    @Test
    void testProxyResponseCopiesItsHeadersDefensively() {
        Map<String, List<String>> headers = new HashMap<>(Map.of("h", List.of("1")));

        ProxyResponse proxyResponse = new ProxyResponse(200, headers, "body", BodyEncoding.BASE64);

        headers.clear();

        assertThat(proxyResponse.headers()).isEqualTo(Map.of("h", List.of("1")));
        assertThat(proxyResponse.bodyEncoding()).isEqualTo(BodyEncoding.BASE64);
    }
}
