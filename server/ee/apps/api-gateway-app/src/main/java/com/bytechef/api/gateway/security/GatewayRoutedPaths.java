/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.security;

import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.PredicateProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.stereotype.Component;

/**
 * The path patterns the gateway proxies to other services, read from the configured routes. Routes that forward to the
 * gateway itself are not proxied and are left out.
 *
 * @version ee
 */
@Component
public class GatewayRoutedPaths implements SpaWebFilterContributor {

    private static final String FORWARD_SCHEME = "forward";

    private final List<String> pathPatterns;

    public GatewayRoutedPaths(GatewayMvcProperties gatewayMvcProperties) {
        Set<String> pathPatterns = new LinkedHashSet<>();

        for (RouteProperties routeProperties : gatewayMvcProperties.getRoutes()) {
            URI uri = routeProperties.getUri();

            if (uri != null && FORWARD_SCHEME.equals(uri.getScheme())) {
                continue;
            }

            for (PredicateProperties predicateProperties : routeProperties.getPredicates()) {
                if ("Path".equals(predicateProperties.getName())) {
                    Map<String, String> args = predicateProperties.getArgs();

                    for (String pathPattern : args.values()) {
                        pathPatterns.add(pathPattern.trim());
                    }
                }
            }
        }

        this.pathPatterns = List.copyOf(pathPatterns);
    }

    @Override
    public List<String> getNonSpaPathPrefixes() {
        Set<String> nonSpaPathPrefixes = new LinkedHashSet<>();

        for (String pathPattern : pathPatterns) {
            nonSpaPathPrefixes.add(getLiteralPrefix(pathPattern));
        }

        return new ArrayList<>(nonSpaPathPrefixes);
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    private static String getLiteralPrefix(String pathPattern) {
        StringBuilder literalPrefix = new StringBuilder();

        for (String segment : pathPattern.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }

            if (segment.contains("*") || segment.contains("{")) {
                break;
            }

            literalPrefix.append('/')
                .append(segment);
        }

        return literalPrefix.isEmpty() ? "/" : literalPrefix.toString();
    }
}
