/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cloud.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CloudCrossOriginConfigurationTest {

    private final CorsFilter corsFilter = new CloudCrossOriginConfiguration().corsFilter();

    @Test
    void testCrossOriginResponseExposesCsrfRejectedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/automation/internal/projects");

        request.addHeader(HttpHeaders.ORIGIN, "https://app.bytechef.io");

        MockHttpServletResponse response = new MockHttpServletResponse();

        corsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://app.bytechef.io");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("X-CSRF-Rejected");
    }
}
