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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.web.authentication.CredentialAuthenticationSuccessEvent;
import com.bytechef.platform.security.web.authentication.CredentialType;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.rememberme.PersistentTokenRememberMeServices;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * @author Ivica Cardic
 */
class SecurityConfigurationIntTest {

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @AutoConfigureMockMvc
    @EnableConfigurationProperties(ApplicationProperties.class)
    @SpringBootTest(
        classes = {
            SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class,
            FrameAncestors.FrameAncestorsConfiguration.class
        })
    class FrameAncestors {

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
        void testFrameablePageRestrictsFrameAncestorsToConfiguredOrigins(String path) throws Exception {
            mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Frame-Options"))
                .andExpect(
                    header().string("Content-Security-Policy", "frame-ancestors https://a.example https://b.example"));
        }

        @Test
        void testIndexPageHasNoFrameAncestorsDirective() throws Exception {
            mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
        }

        @TestConfiguration
        static class FrameAncestorsConfiguration {

            @Bean
            List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
                return List.of(new AuthorizeHttpRequestContributor() {

                    @Override
                    public List<String> getFrameAncestors() {
                        return List.of("https://a.example", "https://b.example");
                    }

                    @Override
                    public List<String> getFrameablePermitAllRequestMatcherPaths() {
                        return List.of("/automation-hub.html", "/integration-marketplace.html",
                            "/workflow-builder.html");
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
                String automationHub() {
                    return "automation-hub";
                }

                @GetMapping("/index.html")
                String index() {
                    return "index";
                }

                @GetMapping("/integration-marketplace.html")
                String integrationMarketplace() {
                    return "integration-marketplace";
                }

                @GetMapping("/workflow-builder.html")
                String workflowBuilder() {
                    return "workflow-builder";
                }
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @AutoConfigureMockMvc
    @EnableConfigurationProperties(ApplicationProperties.class)
    @SpringBootTest(
        classes = {
            SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class,
            FrameablePage.FrameablePageConfiguration.class
        })
    class FrameablePage {

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
                .andExpect(header().doesNotExist("X-Frame-Options"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
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
        static class FrameablePageConfiguration {

            @Bean
            List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
                return List.of(new AuthorizeHttpRequestContributor() {

                    @Override
                    public List<String> getFrameablePermitAllRequestMatcherPaths() {
                        return List.of("/automation-hub.html", "/integration-marketplace.html",
                            "/workflow-builder.html");
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
                String automationHub() {
                    return "automation-hub";
                }

                @GetMapping("/index.html")
                String index() {
                    return "index";
                }

                @GetMapping("/integration-marketplace.html")
                String integrationMarketplace() {
                    return "integration-marketplace";
                }

                @GetMapping("/other.html")
                String other() {
                    return "other";
                }

                @GetMapping("/workflow-builder.html")
                String workflowBuilder() {
                    return "workflow-builder";
                }
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @AutoConfigureMockMvc
    @EnableConfigurationProperties(ApplicationProperties.class)
    @SpringBootTest(
        classes = {
            SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class,
            RememberMeAutoLogin.RememberMeAutoLoginConfiguration.class
        })
    @TestPropertySource(properties = "bytechef.security.login.enabled=true")
    class RememberMeAutoLogin {

        private static final String COOKIE_TENANT_ID = "000003";
        private static final String FIRST_TENANT_ID = "000002";
        private static final String SERIES = "series";
        private static final String TOKEN_VALUE = "token-value";

        @MockitoBean
        private AuthenticationFailureHandler authenticationFailureHandler;

        @MockitoBean
        private AuthenticationSuccessHandler authenticationSuccessHandler;

        @Autowired
        private AuthenticationSuccessEventRecorder authenticationSuccessEventRecorder;

        @MockitoBean(name = "corsConfigurationSource")
        private CorsConfigurationSource corsConfigurationSource;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PasswordEncoder passwordEncoder;

        @MockitoBean
        private PersistentTokenService persistentTokenService;

        @MockitoBean
        private TenantService tenantService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        @MockitoBean
        private UserService userService;

        @Test
        void testRememberMeAutoLoginAuthenticationSuccessEventCarriesTheCookieTenant() throws Exception {
            User user = new User();

            user.setId(1L);
            user.setLogin("jane");

            PersistentToken persistentToken = new PersistentToken();

            persistentToken.setSeries(SERIES);
            persistentToken.setTokenDate(LocalDate.now());
            persistentToken.setTokenValue(TOKEN_VALUE);
            persistentToken.setUser(user);

            when(tenantService.isMultiTenantEnabled()).thenReturn(true);
            when(tenantService.getTenantIdsByUserLogin("jane")).thenReturn(List.of(FIRST_TENANT_ID));
            when(persistentTokenService.fetchPersistentToken(SERIES)).thenReturn(Optional.of(persistentToken));
            when(userService.getUser(1L)).thenReturn(user);
            when(userDetailsService.loadUserByUsername("jane"))
                .thenReturn(
                    new org.springframework.security.core.userdetails.User(
                        "jane", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            authenticationSuccessEventRecorder.clear();

            mockMvc.perform(
                get("/api/remember-me-probe")
                    .cookie(new Cookie("remember-me", encodeCookie(SERIES, TOKEN_VALUE, COOKIE_TENANT_ID)))
                    .header("User-Agent", "Test agent"))
                .andExpect(status().isOk());

            Authentication authentication = authenticationSuccessEventRecorder.getAuthentication();

            assertThat(authentication).isInstanceOf(RememberMeAuthenticationToken.class);
            assertThat(authentication.getName()).isEqualTo("jane");
            assertThat(authentication.getDetails())
                .isInstanceOfSatisfying(
                    TenantWebAuthenticationDetails.class,
                    details -> assertThat(details.getTenantId()).isEqualTo(COOKIE_TENANT_ID));
        }

        private static String encodeCookie(String... cookieTokens) {
            List<String> encodedCookieTokens = Arrays.stream(cookieTokens)
                .map(cookieToken -> URLEncoder.encode(cookieToken, StandardCharsets.UTF_8))
                .toList();

            Base64.Encoder encoder = Base64.getEncoder();

            String cookieValue = String.join(":", encodedCookieTokens);

            return encoder.encodeToString(cookieValue.getBytes(StandardCharsets.UTF_8));
        }

        @TestConfiguration
        static class RememberMeAutoLoginConfiguration {

            @Bean
            AuthenticationSuccessEventRecorder authenticationSuccessEventRecorder() {
                return new AuthenticationSuccessEventRecorder();
            }

            @Bean
            List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
                return List.of();
            }

            @Bean
            RememberMeServices rememberMeServices(
                RememberMeKey rememberMeKey, UserDetailsService userDetailsService,
                PersistentTokenService persistentTokenService, TenantService tenantService) {

                return new PersistentTokenRememberMeServices(
                    rememberMeKey, userDetailsService, persistentTokenService, tenantService);
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
            static class RememberMeProbeController {

                @GetMapping("/api/remember-me-probe")
                String rememberMeProbe() {
                    return "ok";
                }
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @AutoConfigureMockMvc
    @EnableConfigurationProperties(ApplicationProperties.class)
    @SpringBootTest(
        classes = {
            SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class,
            ActuatorHttpBasic.ActuatorHttpBasicConfiguration.class
        },
        properties = {
            "bytechef.security.system.username=" + ActuatorHttpBasic.SYSTEM_USERNAME,
            "bytechef.security.system.password=" + ActuatorHttpBasic.SYSTEM_PASSWORD
        })
    class ActuatorHttpBasic {

        private static final String SYSTEM_PASSWORD = "system-password";
        private static final String SYSTEM_USERNAME = "system_admin";

        @Autowired
        private ApplicationEventRecorder applicationEventRecorder;

        @MockitoBean
        private AuthenticationFailureHandler authenticationFailureHandler;

        @MockitoBean
        private AuthenticationSuccessHandler authenticationSuccessHandler;

        @MockitoBean(name = "corsConfigurationSource")
        private CorsConfigurationSource corsConfigurationSource;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private RememberMeServices rememberMeServices;

        @Test
        void testSystemUserPublishesCredentialAuthenticationSuccessEventInsteadOfLogin() throws Exception {
            applicationEventRecorder.clear();

            mockMvc.perform(get("/actuator/probe").with(httpBasic(SYSTEM_USERNAME, SYSTEM_PASSWORD)))
                .andExpect(status().isOk());

            assertThat(applicationEventRecorder.getEvents(AbstractAuthenticationEvent.class)).isEmpty();
            assertThat(applicationEventRecorder.getEvents(CredentialAuthenticationSuccessEvent.class))
                .singleElement()
                .satisfies(credentialAuthenticationSuccessEvent -> {
                    Authentication authentication = credentialAuthenticationSuccessEvent.getAuthentication();

                    assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
                    assertThat(authentication.getName()).isEqualTo(SYSTEM_USERNAME);
                    assertThat(credentialAuthenticationSuccessEvent.getCredentialType())
                        .isEqualTo(CredentialType.HTTP_BASIC);
                });
        }

        @Test
        void testWrongSystemPasswordPublishesAuthenticationFailureEvent() throws Exception {
            applicationEventRecorder.clear();

            mockMvc.perform(get("/actuator/probe").with(httpBasic(SYSTEM_USERNAME, "wrong-password")))
                .andExpect(status().isUnauthorized());

            assertThat(applicationEventRecorder.getEvents(CredentialAuthenticationSuccessEvent.class)).isEmpty();
            assertThat(applicationEventRecorder.getEvents(AuthenticationFailureBadCredentialsEvent.class))
                .singleElement()
                .satisfies(authenticationFailureBadCredentialsEvent -> {
                    Authentication authentication = authenticationFailureBadCredentialsEvent.getAuthentication();

                    assertThat(authentication.getName()).isEqualTo(SYSTEM_USERNAME);
                });
        }

        @TestConfiguration
        static class ActuatorHttpBasicConfiguration {

            @Bean
            ApplicationEventRecorder applicationEventRecorder() {
                return new ApplicationEventRecorder();
            }

            @Bean
            AuthenticationEventPublisher authenticationEventPublisher(
                ApplicationEventPublisher applicationEventPublisher) {

                return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
            }

            @Bean
            List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
                return List.of();
            }

            @Bean
            PasswordEncoder passwordEncoder() {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder();
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
            static class ActuatorProbeController {

                @GetMapping("/actuator/probe")
                String actuatorProbe() {
                    return "ok";
                }
            }
        }
    }

    static class ApplicationEventRecorder implements ApplicationListener<ApplicationEvent> {

        private final List<ApplicationEvent> applicationEvents = new CopyOnWriteArrayList<>();

        @Override
        public void onApplicationEvent(ApplicationEvent applicationEvent) {
            applicationEvents.add(applicationEvent);
        }

        void clear() {
            applicationEvents.clear();
        }

        <T extends ApplicationEvent> List<T> getEvents(Class<T> eventClass) {
            return applicationEvents.stream()
                .filter(eventClass::isInstance)
                .map(eventClass::cast)
                .toList();
        }
    }

    static class AuthenticationSuccessEventRecorder implements ApplicationListener<AuthenticationSuccessEvent> {

        private final List<AuthenticationSuccessEvent> authenticationSuccessEvents = new CopyOnWriteArrayList<>();

        @Override
        public void onApplicationEvent(AuthenticationSuccessEvent authenticationSuccessEvent) {
            authenticationSuccessEvents.add(authenticationSuccessEvent);
        }

        void clear() {
            authenticationSuccessEvents.clear();
        }

        Authentication getAuthentication() {
            return authenticationSuccessEvents.stream()
                .map(AuthenticationSuccessEvent::getAuthentication)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No AuthenticationSuccessEvent was published"));
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @AutoConfigureMockMvc
    @EnableConfigurationProperties(ApplicationProperties.class)
    @SpringBootTest(
        classes = {
            SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class, LoginConfiguration.class
        })
    @TestPropertySource(properties = "bytechef.security.login.enabled=false")
    class LoginDisabled {

        @MockitoBean(name = "corsConfigurationSource")
        private CorsConfigurationSource corsConfigurationSource;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PasswordEncoder passwordEncoder;

        @Test
        void testContextStartsWithoutUserServices() {
        }

        @Test
        void testLoginEndpointIsNotServed() throws Exception {
            mockMvc.perform(post("/api/authentication")
                .param("username", "admin")
                .param("password", "admin")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void testRememberMeCookieIsIgnored() throws Exception {
            mockMvc.perform(get("/api/ping").cookie(new Cookie("remember-me", "abc")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
        }

        @Test
        void testSessionAuthenticationIsHonored() throws Exception {
            mockMvc.perform(get("/api/ping").with(user("admin")))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @AutoConfigureMockMvc
    @EnableConfigurationProperties(ApplicationProperties.class)
    @SpringBootTest(
        classes = {
            SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class, LoginConfiguration.class
        })
    @TestPropertySource(properties = "bytechef.security.login.enabled=true")
    class LoginEnabled {

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

        @Test
        void testLoginEndpointIsServed() throws Exception {
            mockMvc.perform(post("/api/authentication")
                .param("username", "admin")
                .param("password", "admin")
                .with(csrf()))
                .andExpect(status().isOk());

            verify(authenticationFailureHandler).onAuthenticationFailure(any(), any(), any());
        }
    }

    @TestConfiguration
    static class LoginConfiguration {

        @Bean
        List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors() {
            return List.of();
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
        static class PingController {

            @GetMapping("/api/ping")
            String ping() {
                return "pong";
            }
        }
    }
}
