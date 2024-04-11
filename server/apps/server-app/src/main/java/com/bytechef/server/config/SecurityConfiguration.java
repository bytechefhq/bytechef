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

package com.bytechef.server.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import com.bytechef.platform.user.config.SecurityProperties;
import com.bytechef.platform.user.security.constant.AuthoritiesConstants;
import com.bytechef.platform.web.rest.filter.SpaWebFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * @author Ivica Cardic
 */
@EnableConfigurationProperties(SecurityProperties.class)
@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final RememberMeServices rememberMeServices;
    private final SecurityProperties securityProperties;

    @SuppressFBWarnings("EI")
    public SecurityConfiguration(RememberMeServices rememberMeServices, SecurityProperties securityProperties) {
        this.rememberMeServices = rememberMeServices;
        this.securityProperties = securityProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            .cors(withDefaults())
            // TODO
//            .csrf(csrf -> csrf
//                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                // See https://stackoverflow.com/q/74447118/65681
//                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
//            .addFilterAfter(new CookieCsrfFilter(), BasicAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable)
            .headers(
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
                authz -> authz
                    .requestMatchers(mvc.pattern("/index.html"), mvc.pattern("/assets/**"))
                    .permitAll()
                    .requestMatchers(
                        mvc.pattern("/*.ico"), mvc.pattern("/*.png"), mvc.pattern("/*.svg"), mvc.pattern("/*.webapp"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/app/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/i18n/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/content/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/swagger-ui/**"))
                    .permitAll()
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
                    .requestMatchers(mvc.pattern("/api/admin/**"))
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/api/**"))
                    .permitAll()
//                        .requestMatchers(mvc.pattern("/api/**")).authenticated()
                    .requestMatchers(mvc.pattern("/management/health"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/health/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/info"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/prometheus"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/v3/api-docs/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/webhooks/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/**"))
                    .hasAuthority(AuthoritiesConstants.ADMIN))
            .rememberMe(
                rememberMe -> rememberMe
                    .rememberMeServices(rememberMeServices)
                    .rememberMeParameter("remember-me")
                    .key(securityProperties.getRememberMe()
                        .getKey()))
            .exceptionHandling(
                exceptionHanding -> exceptionHanding.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new OrRequestMatcher(antMatcher("/api/**"))))
            .formLogin(
                formLogin -> formLogin
                    .loginPage("/")
                    .loginProcessingUrl("/api/authentication")
                    .successHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value()))
                    .failureHandler(
                        (request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                    .permitAll())
            .logout(
                logout -> logout.logoutUrl("/api/logout")
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                    .permitAll());

        return http.build();
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
}
