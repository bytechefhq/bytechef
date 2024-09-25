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

package com.bytechef.embedded.unified.web.pagination;

import com.bytechef.embedded.unified.pagination.CursorPageRequest;
import com.bytechef.embedded.unified.pagination.CursorPageable;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Extracts paging information from web requests and thus allows injecting {@link CursorPageable} instances into
 * controller methods. Request properties to be parsed can be configured.
 *
 * @author Davide Pedone
 * @since 1.1
 */
public class CursorPageableHandlerMethodArgumentResolver extends CursorPageableHandlerMethodArgumentResolverSupport
    implements CursorPageableArgumentResolver {

    private static final SortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER =
        new SortHandlerMethodArgumentResolver();
    private final SortArgumentResolver sortResolver;

    public CursorPageableHandlerMethodArgumentResolver() {
        this((SortArgumentResolver) null);
    }

    public CursorPageableHandlerMethodArgumentResolver(SortHandlerMethodArgumentResolver sortResolver) {
        this((SortArgumentResolver) sortResolver);
    }

    public CursorPageableHandlerMethodArgumentResolver(@Nullable SortArgumentResolver sortResolver) {
        this.sortResolver = sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
    }

    @Override
    public CursorPageable resolveArgument(
        MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer,
        NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) {

        String continuationToken = nativeWebRequest.getParameter(
            getParameterNameToUse(getContinuationTokenParameterName(), methodParameter));
        String pageSize = nativeWebRequest.getParameter(getParameterNameToUse(getSizeParameterName(), methodParameter));
        Sort sort = sortResolver.resolveArgument(
            methodParameter, modelAndViewContainer, nativeWebRequest, webDataBinderFactory);

        CursorPageable pageable = getPageable(methodParameter, pageSize, continuationToken);

        if (sort.isSorted()) {
            List<Sort.Order> list = sort.toList();

            Sort.Order order = list.getFirst();

            return CursorPageRequest.of(
                pageable.getContinuationToken(), pageable.getSize(), order.getProperty(), order.getDirection());
        }

        return pageable;
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return CursorPageable.class.equals(methodParameter.getParameterType());
    }
}
