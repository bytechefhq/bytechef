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

import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security;
import com.bytechef.config.ApplicationProperties.Security.RememberMe;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.web.config.AuthenticationProviderContributor;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.CsrfContributor;
import com.bytechef.platform.security.web.config.FilterAfterContributor;
import com.bytechef.platform.security.web.config.FilterBeforeContributor;
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import com.bytechef.security.web.filter.CookieCsrfFilter;
import com.bytechef.security.web.filter.SpaWebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final RememberMeServices rememberMeServices;
    private final Security security;

    public SecurityConfiguration(
        ApplicationProperties applicationProperties, AuthenticationFailureHandler authenticationFailureHandler,
        AuthenticationSuccessHandler authenticationSuccessHandler, RememberMeServices rememberMeServices) {

        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.rememberMeServices = rememberMeServices;
        this.security = applicationProperties.getSecurity();
    }

    /**
     * Configures the security filter chain for the actuator endpoints, specifying authorization rules, authentication
     * mechanisms, and exception handling.
     *
     * @param http the {@link HttpSecurity} object used to customize security settings for the actuator endpoints
     * @param mvc  a {@link PathPatternRequestMatcher.Builder} used to create matchers for specific URI patterns
     * @return a configured {@link SecurityFilterChain} to handle security for actuator endpoints
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
    @Order(2)
    public SecurityFilterChain actuatorFilterChain(
        HttpSecurity http, PathPatternRequestMatcher.Builder mvc) throws Exception {

        http
            .securityMatcher("/actuator/**")
            .cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(mvc.matcher("/actuator/health"))
                .permitAll()
                .requestMatchers(mvc.matcher("/actuator/health/**"))
                .permitAll()
                .requestMatchers(mvc.matcher("/actuator/info"))
                .permitAll()
                .requestMatchers(mvc.matcher("/actuator/metrics"))
                .permitAll()
                .requestMatchers(mvc.matcher("/actuator/metrics/**"))
                .permitAll()
                .requestMatchers(mvc.matcher("/actuator/prometheus"))
                .permitAll()
                .requestMatchers(mvc.matcher("/actuator/**"))
                .hasAuthority(AuthorityConstants.SYSTEM_ADMIN))
            .httpBasic(withDefaults())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new UnauthorizedBasicAuthenticationEntryPoint()));

        AuthenticationProvider authenticationProvider = getSystemAuthenticationProvider(security.getSystem());

        if (authenticationProvider != null) {
            http.authenticationProvider(authenticationProvider);
        }

        return http.build();
    }

    /**
     * Configures the security filter chain for API endpoints and GraphQL requests, defining authorization,
     * authentication, CSRF settings, and headers for securing requests.
     *
     * @param http                               the {@link HttpSecurity} object used to customize the security settings
     *                                           for the application.
     * @param mvc                                a {@link PathPatternRequestMatcher.Builder} used to build request
     *                                           matchers.
     * @param authenticationProviderContributors a list of {@link AuthenticationProviderContributor} instances
     *                                           contributing custom {@link AuthenticationProvider}s to handle
     *                                           authentication.
     * @param authorizeHttpRequestContributors   a list of {@link AuthorizeHttpRequestContributor} instances providing
     *                                           paths to be configured as permit-all in the API security configuration.
     * @param csrfContributors                   a list of {@link CsrfContributor} instances contributing request
     *                                           matchers to be ignored for CSRF protection.
     * @param environment                        the {@link Environment} object used to retrieve profiles and
     *                                           environment properties.
     * @param filterAfterContributors            a list of {@link FilterAfterContributor} instances allowing additional
     *                                           filters to be added after default filters in the chain.
     * @param filterBeforeContributors           a list of {@link FilterBeforeContributor} instances allowing additional
     *                                           filters to be added before default filters in the chain.
     * @param spaWebFilterContributors           a list of {@link SpaWebFilterContributor} instances contributing to the
     *                                           customization of SPA-specific filters.
     * @return a configured {@link SecurityFilterChain} for securing API and GraphQL endpoints.
     * @throws Exception if an error occurs while configuring the security filter chain.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(
        HttpSecurity http, PathPatternRequestMatcher.Builder mvc,
        List<AuthenticationProviderContributor> authenticationProviderContributors,
        List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors, List<CsrfContributor> csrfContributors,
        Environment environment, List<FilterAfterContributor> filterAfterContributors,
        List<FilterBeforeContributor> filterBeforeContributors, List<SpaWebFilterContributor> spaWebFilterContributors)
        throws Exception {

        http
            .securityMatcher("/api/**", "/graphql")
            .cors(withDefaults())
            .csrf(csrf -> {
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    // See https://stackoverflow.com/q/74447118/65681
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler());

                for (CsrfContributor csrfContributor : csrfContributors) {
                    for (RequestMatcher requestMatcher : csrfContributor.getIgnoringRequestMatchers()) {
                        csrf.ignoringRequestMatchers(requestMatcher);
                    }
                }

                csrf
                    // For CORS requests
                    .ignoringRequestMatchers(request -> Objects.equals(request.getMethod(), "OPTIONS"))
                    // For internal calls from the swagger UI in the dev profile
                    .ignoringRequestMatchers(request -> environment.acceptsProfiles(Profiles.of("dev")) &&
                        StringUtils.contains(request.getHeader("Referer"), "/swagger-ui/"));
            });

        for (AuthenticationProviderContributor authenticationProviderContributor : authenticationProviderContributors) {
            http.authenticationProvider(authenticationProviderContributor.getAuthenticationProvider());
        }

        http.addFilterAfter(new SpaWebFilter(spaWebFilterContributors), BasicAuthenticationFilter.class)
            .addFilterAfter(new CookieCsrfFilter(), BasicAuthenticationFilter.class);

        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(security.getContentSecurityPolicy()))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(permissions -> permissions
                    .policy(
                        "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()")))
            .authorizeHttpRequests(authz -> {
                for (AuthorizeHttpRequestContributor authorizeHttpRequestContributor : authorizeHttpRequestContributors) {
                    for (String path : authorizeHttpRequestContributor.getApiPermitAllRequestMatcherPaths()) {
                        authz
                            .requestMatchers(mvc.matcher(path))
                            .permitAll();
                    }
                }

                authz
                    .requestMatchers(mvc.matcher("/api/**"))
                    .authenticated()
                    .requestMatchers(mvc.matcher("/graphql"))
                    .authenticated();
            })
            .rememberMe(rememberMe -> rememberMe
                .rememberMeServices(rememberMeServices)
                .rememberMeParameter("remember-me")
                .key(getRememberMeKey()))
            .exceptionHandling(exceptionHanding -> exceptionHanding
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new OrRequestMatcher(mvc.matcher("/api/**"), mvc.matcher("/graphql"))))
            .formLogin(formLogin -> formLogin
                .loginPage("/")
                .loginProcessingUrl("/api/authentication")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                .permitAll());

        http.with(new FilterAfterContributorConfigurer<>(filterAfterContributors), withDefaults());
        http.with(new FilterBeforeContributorConfigurer<>(filterBeforeContributors), withDefaults());

        return http.build();
    }

    /**
     * Configures the security filter chain for the web application, defining authorization, authentication, and the
     * integration of SPA-specific and permit-all contributors.
     *
     * @param http                             the {@link HttpSecurity} object used to customize security settings for
     *                                         the application
     * @param mvc                              a {@link PathPatternRequestMatcher.Builder} used to create request
     *                                         matchers for specific URI patterns
     * @param authorizeHttpRequestContributors a list of {@link AuthorizeHttpRequestContributor} instances providing
     *                                         paths to be configured as permit-all in the security configuration
     *
     * @param spaWebFilterContributors         a list of {@link SpaWebFilterContributor} instances contributing to the
     *                                         customization of SPA-specific filters
     * @return a configured {@link SecurityFilterChain} for managing security in the application
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
    @Order(4)
    public SecurityFilterChain filterChain(
        HttpSecurity http, PathPatternRequestMatcher.Builder mvc,
        List<AuthorizeHttpRequestContributor> authorizeHttpRequestContributors,
        List<SpaWebFilterContributor> spaWebFilterContributors) throws Exception {

        http
            .addFilterAfter(new SpaWebFilter(spaWebFilterContributors), BasicAuthenticationFilter.class)
            .cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> {
                for (AuthorizeHttpRequestContributor authorizeHttpRequestContributor : authorizeHttpRequestContributors) {
                    for (String path : authorizeHttpRequestContributor.getPermitAllRequestMatcherPaths()) {
                        authz
                            .requestMatchers(mvc.matcher(path))
                            .permitAll();
                    }
                }

                authz
                    .requestMatchers(mvc.matcher("/*.ico"), mvc.matcher("/*.png"), mvc.matcher("/*.svg"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/assets/**"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/i18n/**"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/icons/**"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/index.html"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/swagger-ui/**"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/swagger-ui.html"))
                    .permitAll()
                    .requestMatchers(mvc.matcher("/v3/api-docs/**"))
                    .permitAll()
                    .anyRequest()
                    .denyAll();
            });

        return http.build();
    }

    /**
     * Configures the security filter chain for GraphQL and GraphiQL endpoints in the development profile, defining
     * authorization rules, authentication mechanisms, and exception handling.
     *
     * @param http the {@link HttpSecurity} object used to customize security settings for the GraphQL endpoints
     * @param mvc  a {@link PathPatternRequestMatcher.Builder} used to create request matchers for specific URI patterns
     * @return a configured {@link SecurityFilterChain} for securing GraphQL and GraphiQL endpoints
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
    @Profile("dev")
    @Order(1)
    public SecurityFilterChain graphqlDevFilterChain(HttpSecurity http, PathPatternRequestMatcher.Builder mvc)
        throws Exception {

        http
            .securityMatcher("/graphql", "/graphiql")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(mvc.matcher("/graphiql"))
                .permitAll()
                .requestMatchers(mvc.matcher("/graphql"))
                .authenticated())
            .httpBasic(withDefaults())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new UnauthorizedBasicAuthenticationEntryPoint()));

        return http.build();
    }

    @Bean
    PathPatternRequestMatcher.Builder mvc() {
        return PathPatternRequestMatcher.withDefaults();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private DaoAuthenticationProvider getSystemAuthenticationProvider(Security.System system) {
        String password = system.getPassword();
        String username = system.getUsername();

        if (password == null || password.isBlank() || username == null || username.isBlank()) {
            return null;
        }

        PasswordEncoder passwordEncoder = passwordEncoder();

        UserDetails user = User.withUsername(system.getUsername())
            .password(passwordEncoder.encode(system.getPassword()))
            .authorities(AuthorityConstants.SYSTEM_ADMIN)
            .build();

        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(
            new InMemoryUserDetailsManager(user));

        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

        return daoAuthenticationProvider;
    }

    private String getRememberMeKey() {
        RememberMe rememberMe = security.getRememberMe();

        return rememberMe.getKey();
    }

    /**
     * A configuration class for adding custom filters to the security filter chain after specified filters. This class
     * allows customization of the filter chain by applying a list of {@link FilterAfterContributor} instances.
     *
     * @param <H> the type of {@link HttpSecurityBuilder} used for configuring the security filter chain
     */
    private static class FilterAfterContributorConfigurer<H extends HttpSecurityBuilder<HttpSecurity>>
        extends AbstractHttpConfigurer<FilterBeforeContributorConfigurer<H>, HttpSecurity> {

        private final List<FilterAfterContributor> filterAfterContributors;

        FilterAfterContributorConfigurer(List<FilterAfterContributor> filterAfterContributors) {
            this.filterAfterContributors = filterAfterContributors;
        }

        @Override
        public void configure(HttpSecurity http) {
            for (FilterAfterContributor filterAfterContributor : filterAfterContributors) {
                http.addFilterAfter(
                    filterAfterContributor.getFilter(),
                    filterAfterContributor.getAfterFilter());
            }
        }
    }

    /**
     * A private configuration class for adding and positioning filters in the web security filter chain before a
     * specific set of filters. This configurer uses a list of {@link FilterBeforeContributor} instances to determine
     * which filters should be introduced into the chain and their corresponding positions.
     *
     * @param <H> the type of {@link HttpSecurityBuilder} used to configure the web security filter chain.
     */
    private static class FilterBeforeContributorConfigurer<H extends HttpSecurityBuilder<HttpSecurity>>
        extends AbstractHttpConfigurer<FilterBeforeContributorConfigurer<H>, HttpSecurity> {

        private final List<FilterBeforeContributor> filterBeforeContributors;

        FilterBeforeContributorConfigurer(List<FilterBeforeContributor> filterBeforeContributors) {
            this.filterBeforeContributors = filterBeforeContributors;
        }

        @Override
        public void configure(HttpSecurity http) {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

            for (FilterBeforeContributor filterBeforeContributor : filterBeforeContributors) {
                http.addFilterBefore(
                    filterBeforeContributor.getFilter(authenticationManager),
                    filterBeforeContributor.getBeforeFilter());
            }
        }
    }

    /**
     * Custom CSRF handler to provide BREACH protection.
     *
     * @see <a href=
     *      "https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa">Spring
     *      Security Documentation - Integrating with CSRF Protection</a>
     * @see <a href="https://github.com/jhipster/generator-jhipster/pull/25907">JHipster - use customized
     *      SpaCsrfTokenRequestHandler to handle CSRF token</a>
     * @see <a href="https://stackoverflow.com/q/74447118/65681">CSRF protection not working with Spring Security 6</a>
     */
    private static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of the CsrfToken when it is
             * rendered in the response body.
             */
            this.delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler to resolve the CsrfToken.
             * This applies when a single-page application includes the header value automatically, which was obtained
             * via a cookie containing the raw CsrfToken.
             */
            if (org.springframework.util.StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, csrfToken);
            }
            /*
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies when a server-side rendered
             * form includes the _csrf request parameter as a hidden input.
             */
            return this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /**
     * A custom implementation of {@link BasicAuthenticationEntryPoint} used to handle unauthorized access attempts when
     * basic authentication is required.
     *
     * This class extends the default functionality of {@link BasicAuthenticationEntryPoint} to customize the behavior
     * for responding to unauthorized requests. It specifically defines the response headers and status code returned to
     * the client upon an authentication failure.
     *
     * Key functionality: - Sets the "WWW-Authenticate" response header to indicate the required basic authentication
     * with a realm. - Responds with the HTTP 401 (Unauthorized) status code to indicate that the request requires
     * authentication.
     *
     * Method: {@link #commence(HttpServletRequest, HttpServletResponse, AuthenticationException)}: - Handles the
     * response when an {@link AuthenticationException} occurs, customizing the headers and status code.
     */
    private static class UnauthorizedBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

        @Override
        public void commence(
            HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {

            response.setHeader("WWW-Authenticate", "Basic realm=\"Protected Endpoints\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
