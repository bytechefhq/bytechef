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

package com.bytechef.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Ivica Cardic
 */
class SpaWebFilterTest {

    private final SpaWebFilter spaWebFilter = new SpaWebFilter(
        List.<SpaWebFilterContributor>of(() -> List.of("/job/resume", "/webhooks")));

    @ParameterizedTest
    @ValueSource(strings = {
        "/", "/automation/projects", "/resume/abc",
        "/resume/v1.1798154265.TURBd01ERTJPamM1TURJNk5EWTBZMk0xTVRRdE.Xk3d9Pq_2-ZbW8yHc1nV0aL7sGt4eRfUiOpJkLmNoQr"
    })
    void testForwardsClientRoutesToIndexHtml(String path) throws ServletException, IOException {
        MockHttpServletResponse response = doFilter(path);

        assertThat(response.getForwardedUrl()).isEqualTo("/index.html");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/index.html", "/favicon.ico", "/assets/index-B1a2c3.js", "/assets/inter.woff2", "/api/platform/internal/x",
        "/job/resume/v1.1798154265.payload.signature", "/webhooks/abc"
    })
    void testPassesFilesAndServerPathsThrough(String path) throws ServletException, IOException {
        MockHttpServletResponse response = doFilter(path);

        assertThat(response.getForwardedUrl()).isNull();
    }

    @Test
    void testPassesThroughPathWithContextPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/assets/main.css");

        request.setContextPath("/app");

        MockHttpServletResponse response = new MockHttpServletResponse();

        spaWebFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isNull();
    }

    private MockHttpServletResponse doFilter(String path) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        spaWebFilter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
