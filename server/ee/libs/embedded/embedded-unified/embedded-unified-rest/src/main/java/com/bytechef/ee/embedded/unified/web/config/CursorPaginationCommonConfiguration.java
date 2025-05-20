/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.config;

import com.bytechef.ee.embedded.unified.web.pagination.CursorPageableHandlerMethodArgumentResolver;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JavaConfig class to register {@link CursorPageableHandlerMethodArgumentResolver}
 *
 * @version ee
 *
 * @author Davide Pedone
 * @since 1.1
 */
@Configuration
public class CursorPaginationCommonConfiguration implements WebMvcConfigurer {

    private final Optional<SortHandlerMethodArgumentResolver> sortHandlerMethodArgumentResolver;

    public CursorPaginationCommonConfiguration(
        Optional<SortHandlerMethodArgumentResolver> sortHandlerMethodArgumentResolver) {

        this.sortHandlerMethodArgumentResolver = sortHandlerMethodArgumentResolver;
    }

    @Bean
    public CursorPageableHandlerMethodArgumentResolver cursorPageableResolver(
        SortHandlerMethodArgumentResolver sortHandlerMethodArgumentResolver) {

        return new CursorPageableHandlerMethodArgumentResolver(sortHandlerMethodArgumentResolver);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(cursorPageableResolver(sortHandlerMethodArgumentResolver.orElse(null)));
    }
}
