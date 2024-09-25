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

package com.bytechef.embedded.unified.web.config;

import com.bytechef.embedded.unified.web.pagination.CursorPageableHandlerMethodArgumentResolver;
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
