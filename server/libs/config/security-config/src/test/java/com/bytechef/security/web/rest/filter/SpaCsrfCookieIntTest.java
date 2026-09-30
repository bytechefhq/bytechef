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

package com.bytechef.security.web.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import com.bytechef.security.config.RememberMeKey;
import com.bytechef.security.config.SecurityConfiguration;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@EnableConfigurationProperties(ApplicationProperties.class)
@SpringBootTest(
    classes = {
        SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class,
        SpaCsrfCookieIntTest.SpaCsrfCookieIntTestConfiguration.class
    })
public class SpaCsrfCookieIntTest {

    private static final String XSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";

    @MockitoBean
    private AuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RememberMeServices rememberMeServices;

    @MockitoBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPageLoadIssuesXsrfCookie() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"))
            .andExpect(cookie().exists(XSRF_TOKEN_COOKIE_NAME))
            .andExpect(cookie().httpOnly(XSRF_TOKEN_COOKIE_NAME, false))
            .andExpect(cookie().path(XSRF_TOKEN_COOKIE_NAME, "/"));
    }

    @Test
    void testFirstFormSubmitAfterPageLoadPassesCsrfCheck() throws Exception {
        MvcResult pageLoadResult = mockMvc.perform(get("/register"))
            .andReturn();

        Cookie xsrfTokenCookie = pageLoadResult.getResponse()
            .getCookie(XSRF_TOKEN_COOKIE_NAME);

        assertThat(xsrfTokenCookie).isNotNull();

        mockMvc
            .perform(
                post("/api/public-form")
                    .cookie(xsrfTokenCookie)
                    .header("X-XSRF-TOKEN", xsrfTokenCookie.getValue()))
            .andExpect(status().isOk());
    }

    @Test
    void testPageLoadKeepsExistingXsrfCookie() throws Exception {
        mockMvc.perform(get("/register").cookie(new Cookie(XSRF_TOKEN_COOKIE_NAME, "existing-token")))
            .andExpect(status().isOk())
            .andExpect(cookie().doesNotExist(XSRF_TOKEN_COOKIE_NAME));
    }

    @Test
    void testFormSubmitWithoutXsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/public-form"))
            .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class SpaCsrfCookieIntTestConfiguration {

        @Bean
        List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
            return List.of(new AuthorizeHttpRequestContributor() {

                @Override
                public List<String> getApiPermitAllRequestMatcherPaths() {
                    return List.of("/api/public-form");
                }
            });
        }

        @Bean
        List<SecurityConfigurerContributor> securityConfigurerContributors() {
            return List.of();
        }

        @Bean
        List<SpaWebFilterContributor> spaWebFilterContributors() {
            return List.of();
        }

        @RestController
        public static class TestController {

            @GetMapping("/index.html")
            public void index() {
            }

            @PostMapping("/api/public-form")
            public void publicForm() {
            }
        }
    }
}
