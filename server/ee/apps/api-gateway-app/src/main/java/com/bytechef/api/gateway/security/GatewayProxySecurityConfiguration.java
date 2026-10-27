/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.PathContainer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Lets requests the gateway proxies pass through untouched. The service that receives them authenticates them, checks
 * CSRF and writes the security headers, so API keys and embedded tokens keep working. A user GraphQL request carries
 * {@value #USER_GRAPHQL_SCHEMA_HEADER} and is handled by the gateway's own API security instead. The error dispatch of
 * a proxied request passes too, so a failed proxy call reports its own status instead of an access denied on the error
 * page; the error dispatch of a request the gateway serves itself stays with the gateway's own security.
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
        List<PathPattern> routedPathPatterns = new ArrayList<>();

        for (String pathPattern : gatewayRoutedPaths.getPathPatterns()) {
            routedPathRequestMatchers.add(PathPatternRequestMatcher.withDefaults()
                .matcher(pathPattern));
            routedPathPatterns.add(PathPatternParser.defaultInstance.parse(pathPattern));
        }

        http
            .securityMatcher(
                new AndRequestMatcher(
                    new OrRequestMatcher(
                        new OrRequestMatcher(routedPathRequestMatchers),
                        new RoutedErrorDispatchRequestMatcher(routedPathPatterns)),
                    new NegatedRequestMatcher(new RequestHeaderRequestMatcher(USER_GRAPHQL_SCHEMA_HEADER))))
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

    private record RoutedErrorDispatchRequestMatcher(List<PathPattern> routedPathPatterns) implements RequestMatcher {

        @Override
        public boolean matches(HttpServletRequest httpServletRequest) {
            if (httpServletRequest.getDispatcherType() != DispatcherType.ERROR ||
                !(httpServletRequest
                    .getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String errorRequestUri)) {

                return false;
            }

            PathContainer errorPathContainer = PathContainer.parsePath(
                StringUtils.removeStart(errorRequestUri, httpServletRequest.getContextPath()));

            return routedPathPatterns.stream()
                .anyMatch(routedPathPattern -> routedPathPattern.matches(errorPathContainer));
        }
    }
}
