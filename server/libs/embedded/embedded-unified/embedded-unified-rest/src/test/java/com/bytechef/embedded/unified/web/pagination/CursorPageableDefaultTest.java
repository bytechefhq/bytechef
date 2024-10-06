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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.bytechef.embedded.unified.pagination.CursorPageRequest;
import com.bytechef.embedded.unified.pagination.CursorPageable;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Base test class to test supporting of a {@link HandlerMethodArgumentResolver} implementation defaulting
 * {@link CursorPageable} method parameters. Expects the {@link HandlerMethodArgumentResolver} to be tested returned
 * from {@link #getResolver()} and expects methods to be present in the controller class returned from
 * {@link #getControllerClass()}. For sample usage see {@link CursorPageableHandlerMethodArgumentResolver}.
 */
@Disabled
abstract class CursorPageableDefaultTest {

    static final int PAGE_SIZE = 12;

    static final String SORT_FIELD = "firstname";
    static final Sort.Direction SORT_DIRECTION_ASC = Sort.Direction.ASC;
    static final Sort.Direction SORT_DIRECTION_DESC = Sort.Direction.DESC;

    static final CursorPageRequest REFERENCE_WITHOUT_SORT = CursorPageRequest.of(PAGE_SIZE);
    static final CursorPageRequest REFERENCE_WITH_SORT_ASC = CursorPageRequest.of(PAGE_SIZE, SORT_FIELD,
        SORT_DIRECTION_ASC);
    static final CursorPageRequest REFERENCE_WITH_SORT_DESC = CursorPageRequest.of(PAGE_SIZE, SORT_FIELD,
        SORT_DIRECTION_DESC);

    protected abstract CursorPageableHandlerMethodArgumentResolver getResolver();

    protected abstract Class<?> getControllerClass();

    @Test
    void testSupportsCursorPageable() {
        assertThat(getResolver().supportsParameter(getParameterOfMethod("supportedMethod"))).isTrue();
    }

    @Test
    void testDoesNotSupportNonCursorPageable() {

        MethodParameter parameter = getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
        assertThat(getResolver().supportsParameter(parameter)).isFalse();
    }

    @Test
    void testReturnsDefaultIfNoRequestParametersAndNoDefault() throws Exception {
        assertSupportedAndResult(getParameterOfMethod("supportedMethod"),
            (CursorPageable) ReflectionTestUtils.getField(getResolver(), "fallbackPageable"));
    }

    @Test
    void testSimpleDefault() throws Exception {
        assertSupportedAndResult(getParameterOfMethod("simpleDefault"), REFERENCE_WITHOUT_SORT);
    }

    @Test
    void testSimpleDefaultWithSort() throws Exception {
        assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithSort"), REFERENCE_WITH_SORT_DESC);
    }

    @Test
    void testSimpleDefaultWithSortAndDirection() throws Exception {
        assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithSortAndDirection"), REFERENCE_WITH_SORT_ASC);
    }

    @Test
    void testSimpleDefaultWithExternalSort() throws Exception {
        assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithExternalSort"), REFERENCE_WITH_SORT_ASC);
    }

    @Test
    void testSimpleDefaultWithContaineredExternalSort() throws Exception {
        assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithContaineredExternalSort"),
            REFERENCE_WITH_SORT_ASC);
    }

    @Test
    void testRejectsInvalidQualifiers() throws Exception {

        MethodParameter parameter = getParameterOfMethod(getControllerClass(), "invalidQualifiers",
            CursorPageable.class, CursorPageable.class);

        HandlerMethodArgumentResolver resolver = getResolver();
        assertThat(resolver.supportsParameter(parameter)).isTrue();

        assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(parameter, null, getWebRequest(), null)) //
            .withMessageContaining("unique");
    }

    @Test
    void testRejectsNoQualifiers() throws Exception {

        MethodParameter parameter = getParameterOfMethod(getControllerClass(), "noQualifiers", CursorPageable.class,
            CursorPageable.class);

        HandlerMethodArgumentResolver resolver = getResolver();
        assertThat(resolver.supportsParameter(parameter)).isTrue();

        assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(parameter, null, getWebRequest(), null)) //
            .withMessageContaining("Ambiguous");
    }

    protected void assertSupportedAndResult(MethodParameter parameter, CursorPageable pageable) throws Exception {
        assertSupportedAndResult(parameter, pageable, getWebRequest());
    }

    protected void assertSupportedAndResult(
        MethodParameter parameter, CursorPageable pageable, HttpServletRequest request) throws Exception {

        assertSupportedAndResult(parameter, pageable, new ServletWebRequest(request));
    }

    protected void assertSupportedAndResult(
        MethodParameter parameter, CursorPageable pageable, NativeWebRequest request) throws Exception {

        assertSupportedAndResult(parameter, pageable, request, getResolver());
    }

    protected void assertSupportedAndResult(
        MethodParameter parameter, CursorPageable pageable, NativeWebRequest request,
        HandlerMethodArgumentResolver resolver) throws Exception {

        assertThat(resolver.supportsParameter(parameter)).isTrue();
        assertThat(resolver.resolveArgument(parameter, null, request, null)).isEqualTo(pageable);
    }

    protected MethodParameter getParameterOfMethod(String name) {
        return getParameterOfMethod(getControllerClass(), name);
    }

    private static MethodParameter getParameterOfMethod(Class<?> controller, String name) {
        return getParameterOfMethod(controller, name, CursorPageable.class);
    }

    static NativeWebRequest getWebRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    static MethodParameter getParameterOfMethod(Class<?> controller, String name, Class<?>... argumentTypes) {

        Method method = getMethod(controller, name, argumentTypes);
        return new MethodParameter(method, 0);
    }

    static Method getMethod(Class<?> controller, String name, Class<?>... argumentTypes) {

        try {
            return controller.getMethod(name, argumentTypes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
