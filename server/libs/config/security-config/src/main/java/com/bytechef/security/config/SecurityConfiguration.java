/*
 * Copyright 2023-present ByteChef Inc.
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
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import com.bytechef.platform.security.web.authentication.AuthenticationProviderContributor;
import com.bytechef.platform.security.web.filter.FilterAfterContributor;
import com.bytechef.platform.security.web.filter.FilterBeforeContributor;
import com.bytechef.platform.security.web.matcher.AuthenticatedRequestMatcherContributor;
import com.bytechef.platform.user.constant.AuthorityConstants;
import com.bytechef.security.web.filter.CookieCsrfFilter;
import com.bytechef.security.web.filter.SpaWebFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * @author Ivica Cardic
 */
@EnableConfigurationProperties(SecurityProperties.class)
@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final RememberMeServices rememberMeServices;
    private final SecurityProperties securityProperties;
    private final List<FilterAfterContributor> filterAfterContributors;
    private final List<FilterBeforeContributor> filterBeforeContributors;
    private final List<AuthenticatedRequestMatcherContributor> authenticatedRequestMatcherContributors;

    @SuppressFBWarnings("EI")
    public SecurityConfiguration(
        AuthenticationFailureHandler authenticationFailureHandler,
        AuthenticationSuccessHandler authenticationSuccessHandler, RememberMeServices rememberMeServices,
        SecurityProperties securityProperties, List<FilterAfterContributor> filterAfterContributors,
        List<FilterBeforeContributor> filterBeforeContributors,
        List<AuthenticatedRequestMatcherContributor> authenticatedRequestMatcherContributors) {

        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.rememberMeServices = rememberMeServices;
        this.securityProperties = securityProperties;
        this.filterAfterContributors = filterAfterContributors;
        this.filterBeforeContributors = filterBeforeContributors;
        this.authenticatedRequestMatcherContributors = authenticatedRequestMatcherContributors;
    }

    @Bean
    public AuthenticationManager authenticationManager(
        HttpSecurity http, List<AuthenticationProviderContributor> authenticationProviderContributors)
        throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(
            AuthenticationManagerBuilder.class);

        for (AuthenticationProviderContributor authenticationProviderContributor : authenticationProviderContributors) {
            http.authenticationProvider(authenticationProviderContributor.getAuthenticationProvider());
        }

        return authenticationManagerBuilder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http, MvcRequestMatcher.Builder mvc, AuthenticationManager authenticationManager)
        throws Exception {

        http
            .authenticationManager(authenticationManager)
            .cors(withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // See https://stackoverflow.com/q/74447118/65681
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()));

        for (FilterBeforeContributor filterBeforeContributor : filterBeforeContributors) {
            http.addFilterBefore(
                filterBeforeContributor.getFilter(authenticationManager), filterBeforeContributor.getBeforeFilter());
        }

        http.addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .addFilterAfter(new CookieCsrfFilter(), BasicAuthenticationFilter.class);

        for (FilterAfterContributor filterAfterContributor : filterAfterContributors) {
            http.addFilterAfter(filterAfterContributor.getFilter(), filterAfterContributor.getAfterFilter());
        }

        http.headers(
            headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(securityProperties.getContentSecurityPolicy()))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .referrerPolicy(
                    referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(
                    permissions -> permissions.policy(
                        "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()")))
            .authorizeHttpRequests(
                authz -> requestMatchers(authz, mvc)
                    .requestMatchers(mvc.pattern("/*.ico"), mvc.pattern("/*.png"), mvc.pattern("/*.svg"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/health"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/health/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/info"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/prometheus"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/**"))
                    .hasAuthority(AuthorityConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/api/authenticate"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/register"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/activate"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/init"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/finish"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/assets/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/i18n/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/index.html"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/swagger-ui/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/swagger-ui.html"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/v3/api-docs/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/webhooks/**"))
                    .permitAll())
            .rememberMe(
                rememberMe -> rememberMe
                    .rememberMeServices(rememberMeServices)
                    .rememberMeParameter("remember-me")
                    .key(getRememberMeKey()))
            .exceptionHandling(
                exceptionHanding -> exceptionHanding.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new OrRequestMatcher(antMatcher("/api/**"))))
            .formLogin(
                formLogin -> formLogin
                    .loginPage("/")
                    .loginProcessingUrl("/api/authentication")
                    .successHandler(authenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler)
                    .permitAll())
            .logout(
                logout -> logout.logoutUrl("/api/logout")
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                    .permitAll());

        return http.build();
    }

    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requestMatchers(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz,
        MvcRequestMatcher.Builder mvc) {

        for (AuthenticatedRequestMatcherContributor authenticatedRequestMatcherContributor : authenticatedRequestMatcherContributors) {
            authz.requestMatchers(authenticatedRequestMatcherContributor.getRequestMatcher(mvc))
                .authenticated();
        }

        return authz;
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    private String getRememberMeKey() {
        SecurityProperties.RememberMe rememberMe = securityProperties.getRememberMe();

        return rememberMe.getKey();
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
    static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

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
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
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
}
