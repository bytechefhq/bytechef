/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.security;

import jakarta.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Lets requests the gateway proxies pass through untouched. The service that receives them authenticates them, checks
 * CSRF and writes the security headers, so API keys and embedded tokens keep working. A user GraphQL request carries
 * {@value #USER_GRAPHQL_SCHEMA_HEADER} and is handled by the gateway's own API security instead. Error dispatches pass
 * too, so a failed proxy call reports its own status instead of an access denied on the error page.
 *
 * @version ee
 */
@Configuration
public class GatewayProxySecurityConfiguration {

    public static final String USER_GRAPHQL_SCHEMA_HEADER = "X-ByteChef-GraphQL-Schema";

    @Bean
    @Order(0)
    SecurityFilterChain proxyFilterChain(HttpSecurity http, GatewayRoutedPaths gatewayRoutedPaths) throws Exception {
        List<RequestMatcher> routedPathRequestMatchers = new ArrayList<>();

        for (String pathPattern : gatewayRoutedPaths.getPathPatterns()) {
            routedPathRequestMatchers.add(PathPatternRequestMatcher.withDefaults()
                .matcher(pathPattern));
        }

        http
            .securityMatcher(
                new OrRequestMatcher(
                    new AndRequestMatcher(
                        new OrRequestMatcher(routedPathRequestMatchers),
                        new NegatedRequestMatcher(new RequestHeaderRequestMatcher(USER_GRAPHQL_SCHEMA_HEADER))),
                    new DispatcherTypeRequestMatcher(DispatcherType.ERROR)))
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .headers(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(sessionManagement -> sessionManagement
                .sessionCreationPolicy(SessionCreationPolicy.NEVER));

        return http.build();
    }
}
