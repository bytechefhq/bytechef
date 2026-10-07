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

package com.bytechef.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.web.bind.annotation.GetMapping;
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
        SecurityConfigurationFrameablePageIntTest.SecurityConfigurationFrameablePageIntTestConfiguration.class
    })
class SecurityConfigurationFrameablePageIntTest {

    @MockitoBean
    private AuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockitoBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RememberMeServices rememberMeServices;

    @ParameterizedTest
    @ValueSource(strings = {
        "/automation-hub.html", "/integration-marketplace.html", "/workflow-builder.html"
    })
    void testFrameablePageIsPermittedWithoutFrameOptions(String path) throws Exception {
        mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("X-Frame-Options"));
    }

    @Test
    void testIndexPageKeepsFrameOptions() throws Exception {
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void testSpaRouteKeepsFrameOptions() throws Exception {
        mockMvc.perform(get("/automation/projects"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void testUnlistedPageIsDenied() throws Exception {
        mockMvc.perform(get("/other.html"))
            .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class SecurityConfigurationFrameablePageIntTestConfiguration {

        @Bean
        List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
            return List.of(new AuthorizeHttpRequestContributor() {

                @Override
                public List<String> getFrameablePermitAllRequestMatcherPaths() {
                    return List.of("/automation-hub.html", "/integration-marketplace.html", "/workflow-builder.html");
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
        static class PageController {

            @GetMapping("/automation-hub.html")
            void automationHub() {
            }

            @GetMapping("/index.html")
            void index() {
            }

            @GetMapping("/integration-marketplace.html")
            void integrationMarketplace() {
            }

            @GetMapping("/other.html")
            void other() {
            }

            @GetMapping("/workflow-builder.html")
            void workflowBuilder() {
            }
        }
    }
}
