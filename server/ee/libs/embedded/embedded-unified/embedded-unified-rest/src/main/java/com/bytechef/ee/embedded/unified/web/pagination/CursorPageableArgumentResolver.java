/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.pagination;

import com.bytechef.ee.embedded.unified.pagination.CursorPageable;
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
 * @version ee
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
