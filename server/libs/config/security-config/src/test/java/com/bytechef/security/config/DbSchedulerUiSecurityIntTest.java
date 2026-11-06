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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@EnableConfigurationProperties(ApplicationProperties.class)
@WithMockUser
@TestPropertySource(properties = "db-scheduler-ui.enabled=true")
@SpringBootTest(
    classes = {
        SecurityConfiguration.class, ApplicationProperties.class,
        DbSchedulerUiSecurityIntTest.DbSchedulerUiSecurityIntTestConfiguration.class
    })
class DbSchedulerUiSecurityIntTest {

    @MockitoBean
    private AuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RememberMeServices rememberMeServices;

    @MockitoBean(name = "corsConfigurationSource")
    private org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void testAnonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/db-scheduler-api/tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegularUserIsForbidden() throws Exception {
        mockMvc.perform(get("/db-scheduler-api/tasks").with(user("user")
            .authorities(() -> "ROLE_USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSystemAdminIsAllowed() throws Exception {
        mockMvc.perform(get("/db-scheduler-api/tasks").with(user("admin")
            .authorities(() -> AuthorityConstants.SYSTEM_ADMIN)))
            .andExpect(status().isOk());
    }

    @TestConfiguration
    static class DbSchedulerUiSecurityIntTestConfiguration {

        @Bean
        List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
            return List.of();
        }

        @Bean
        RememberMeKey rememberMeKey() {
            RememberMeKey rememberMeKey = mock(RememberMeKey.class);

            when(rememberMeKey.getKey()).thenReturn("test-remember-me-key");

            return rememberMeKey;
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
        static class StubDbSchedulerUiController {

            @GetMapping("/db-scheduler-api/tasks")
            public String tasks() {
                return "[]";
            }
        }
    }
}
