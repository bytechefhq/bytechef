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

import com.bytechef.embedded.unified.pagination.CursorPageable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Argument resolver to extract a {@link CursorPageable} object from a {@link NativeWebRequest} for a particular
 * {@link MethodParameter}. A {@link CursorPageableArgumentResolver} can either resolve {@link CursorPageable} itself or
 * wrap another {@link CursorPageableArgumentResolver} to post-process {@link CursorPageable}. {@link CursorPageable}
 * resolution yields either in a {@link CursorPageable} object or {@literal null} if {@link CursorPageable} cannot be
 * resolved.
 *
 * @author Davide Pedone
 * @since 1.1
 */
public interface CursorPageableArgumentResolver extends HandlerMethodArgumentResolver {

    @Override
    CursorPageable resolveArgument(
        MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest,
        WebDataBinderFactory webDataBinderFactory) throws Exception;
}
