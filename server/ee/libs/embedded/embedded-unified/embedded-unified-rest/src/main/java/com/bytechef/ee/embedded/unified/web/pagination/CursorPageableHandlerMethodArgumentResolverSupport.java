/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.pagination;

import com.bytechef.ee.embedded.unified.pagination.CursorPageRequest;
import com.bytechef.ee.embedded.unified.pagination.CursorPageable;
import com.bytechef.ee.embedded.unified.pagination.CursorPageableDefault;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class providing methods for handler method argument resolvers to create paging information from web requests and
 * thus allows injecting {@link CursorPageable} instances into controller methods. Request properties to be parsed can
 * be configured. Default configuration uses request parameters beginning with
 * {@link #DEFAULT_SIZE_PARAMETER}{@link #DEFAULT_QUALIFIER_DELIMITER}.
 *
 * @version ee
 *
 * @author Davide Pedone
 * @since 1.1
 */
public abstract class CursorPageableHandlerMethodArgumentResolverSupport {

    static final CursorPageable DEFAULT_PAGE_REQUEST = CursorPageRequest.of(null, 20, null, Sort.Direction.DESC);

    private static final String INVALID_DEFAULT_PAGE_SIZE =
        "Invalid default page size configured for method %s! Must not be less than one!";
    private static final String DEFAULT_CONTINUATION_TOKEN_PARAMETER = "continuationToken";
    private static final String DEFAULT_SIZE_PARAMETER = "size";
    private static final String DEFAULT_PREFIX = "";
    private static final String DEFAULT_QUALIFIER_DELIMITER = "_";
    private static final int DEFAULT_MAX_PAGE_SIZE = 2000;

    private String continuationTokenParameterName = DEFAULT_CONTINUATION_TOKEN_PARAMETER;
    private String sizeParameterName = DEFAULT_SIZE_PARAMETER;
    private String prefix = DEFAULT_PREFIX;
    private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;
    private CursorPageable fallbackPageable = DEFAULT_PAGE_REQUEST;
    private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;

    /**
     * Configures the {@link CursorPageable} to be used as fallback in case no {@link CursorPageableDefault} or
     * {@link CursorPageableDefault} (the latter only supported in legacy mode) can be found at the method parameter to
     * be resolved.
     * <p>
     * If you set this to {@literal Optional#empty()}, be aware that you controller methods will get {@literal null}
     * handed into them in case no {@link CursorPageable} data can be found in the request. Note, that doing so will
     * require you supply bot the page <em>and</em> the size parameter with the requests as there will be no default for
     * any of the parameters available.
     *
     * @param fallbackPageable the {@link CursorPageable} to be used as general fallback.
     */
    public void setFallbackPageable(CursorPageable fallbackPageable) {
        Assert.notNull(fallbackPageable, "Fallback Pageable must not be null!");

        this.fallbackPageable = fallbackPageable;
    }

    /**
     * Returns whether the given {@link CursorPageable} is the fallback one.
     *
     * @param cursorPageable can be {@literal null}.
     * @return
     */
    public boolean isFallbackPageable(CursorPageable cursorPageable) {
        return fallbackPageable.equals(cursorPageable);
    }

    public String getContinuationTokenParameterName() {
        return continuationTokenParameterName;
    }

    public void setContinuationTokenParameterName(String continuationTokenParameterName) {
        Assert.hasText(continuationTokenParameterName, "ContinuationToken parameter name must not be null or empty!");

        this.continuationTokenParameterName = continuationTokenParameterName;
    }

    public String getSizeParameterName() {
        return sizeParameterName;
    }

    public void setSizeParameterName(String sizeParameterName) {
        Assert.hasText(sizeParameterName, "Size parameter name must not be null or empty!");

        this.sizeParameterName = sizeParameterName;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    /**
     * Configures a general prefix to be prepended to the page number and page size parameters. Useful to namespace the
     * property names used in case they are clashing with ones used by your application. By default, no prefix is used.
     *
     * @param prefix the prefix to be used or {@literal null} to reset to the default.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
    }

    /**
     * The delimiter to be used between the qualifier and the actual page number and size properties. Defaults to
     * {@code _}. So a qualifier of {@code foo} will result in a page number parameter of {@code foo_page}.
     *
     * @param qualifierDelimiter the delimiter to be used or {@literal null} to reset to the default.
     */
    public void setQualifierDelimiter(String qualifierDelimiter) {
        this.qualifierDelimiter = qualifierDelimiter == null ? DEFAULT_QUALIFIER_DELIMITER : qualifierDelimiter;
    }

    /**
     * Returns the name of the request parameter to find the {@link Pageable} information in. Inspects the given
     * {@link MethodParameter} for {@link Qualifier} present and prefixes the given source parameter name with it.
     *
     * @param source    the basic parameter name.
     * @param parameter the {@link MethodParameter} potentially qualified.
     * @return the name of the request parameter.
     */
    protected String getParameterNameToUse(String source, @Nullable MethodParameter parameter) {
        StringBuilder builder = new StringBuilder(prefix);

        Qualifier qualifier = parameter == null ? null : parameter.getParameterAnnotation(Qualifier.class);

        if (qualifier != null) {
            builder.append(qualifier.value());
            builder.append(qualifierDelimiter);
        }

        return builder.append(source)
            .toString();
    }

    private CursorPageable getDefaultFromAnnotationOrFallback(MethodParameter methodParameter) {

        CursorPageableDefault defaults = methodParameter.getParameterAnnotation(CursorPageableDefault.class);

        if (defaults != null) {
            return getDefaultPageRequestFrom(methodParameter, defaults);
        }

        return fallbackPageable;
    }

    private static CursorPageable getDefaultPageRequestFrom(MethodParameter parameter, CursorPageableDefault defaults) {
        int defaultPageSize = getSpecificPropertyOrDefaultFromValue(defaults, "size");

        if (defaultPageSize < 1) {
            Method annotatedMethod = parameter.getMethod();
            throw new IllegalStateException(String.format(INVALID_DEFAULT_PAGE_SIZE, annotatedMethod));
        }

        if (!StringUtils.hasText(defaults.sort())) {
            return CursorPageRequest.of(defaultPageSize);
        }

        return CursorPageRequest.of(defaultPageSize, defaults.sort(), defaults.direction());
    }

    protected CursorPageable getPageable(
        MethodParameter methodParameter, @Nullable String pageSizeString, @Nullable String continuationToken) {

        assertPageableUniqueness(methodParameter);

        Optional<CursorPageable> defaultOrFallback = getDefaultFromAnnotationOrFallback(methodParameter).toOptional();

        Optional<Integer> pageSize = parseAndApplyBoundaries(pageSizeString, maxPageSize, false);

        if (pageSize.isEmpty() && defaultOrFallback.isEmpty()) {
            return CursorPageable.unpaged();
        }

        int ps = pageSize.orElseGet(
            () -> defaultOrFallback.map(CursorPageable::getSize)
                .orElseThrow(IllegalStateException::new));

        // Limit lower bound
        ps = ps < 1 ? defaultOrFallback.map(CursorPageable::getSize)
            .orElseThrow(IllegalStateException::new) : ps;
        // Limit upper bound
        ps = Math.min(ps, maxPageSize);

        return CursorPageRequest.of(continuationToken, ps, defaultOrFallback.map(CursorPageable::getSort)
            .orElse(null),
            defaultOrFallback.map(CursorPageable::getDirection)
                .orElse(CursorPageable.unpaged()
                    .getDirection()));
    }

    /**
     * Tries to parse the given {@link String} into an integer and applies the given boundaries. Will return 0 if the
     * {@link String} cannot be parsed.
     *
     * @param parameter the parameter value.
     * @param upper     the upper bound to be applied.
     * @return
     */
    private Optional<Integer> parseAndApplyBoundaries(@Nullable String parameter, int upper, boolean shiftIndex) {
        if (!StringUtils.hasText(parameter)) {
            return Optional.empty();
        }

        try {
            int parsed = Integer.parseInt(parameter) - (shiftIndex ? 1 : 0);
            return Optional.of(parsed < 0 ? 0 : Math.min(parsed, upper));
        } catch (NumberFormatException e) {
            return Optional.of(0);
        }
    }

    /**
     * Retrieves the maximum page size to be accepted. This allows to put an upper boundary of the page size to prevent
     * potential attacks trying to issue an {@link OutOfMemoryError}. Defaults to {@link #DEFAULT_MAX_PAGE_SIZE}.
     *
     * @return the maximum page size allowed.
     */
    protected int getMaxPageSize() {
        return this.maxPageSize;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getSpecificPropertyOrDefaultFromValue(Annotation annotation, String property) {
        Object propertyDefaultValue = AnnotationUtils.getDefaultValue(annotation, property);
        Object propertyValue = AnnotationUtils.getValue(annotation, property);

        Object result = ObjectUtils.nullSafeEquals(propertyDefaultValue, propertyValue) //
            ? AnnotationUtils.getValue(annotation) //
            : propertyValue;

        if (result == null) {
            throw new IllegalStateException("Exepected to be able to look up an annotation property value but failed!");
        }

        return (T) result;
    }

    /**
     * Asserts uniqueness of all {@link CursorPageable} parameters of the method of the given {@link MethodParameter}.
     *
     * @param parameter must not be {@literal null}.
     */
    public static void assertPageableUniqueness(MethodParameter parameter) {
        Method method = parameter.getMethod();

        if (method == null) {
            throw new IllegalArgumentException(
                String.format("Method parameter %s is not backed by a method.", parameter));
        }

        if (containsMoreThanOnePageableParameter(method)) {
            Annotation[][] annotations = method.getParameterAnnotations();

            assertQualifiersFor(method.getParameterTypes(), annotations);
        }
    }

    /**
     * Returns whether the given {@link Method} has more than one {@link Pageable} parameter.
     *
     * @param method must not be {@literal null}.
     * @return
     */
    private static boolean containsMoreThanOnePageableParameter(Method method) {

        boolean pageableFound = false;

        for (Class<?> type : method.getParameterTypes()) {
            if (pageableFound && type.equals(CursorPageable.class)) {
                return true;
            }

            if (type.equals(CursorPageable.class)) {
                pageableFound = true;
            }
        }

        return false;
    }

    /**
     * Asserts that every {@link CursorPageable} parameter of the given parameters carries an {@link Qualifier}
     * annotation to distinguish them from each other.
     *
     * @param parameterTypes must not be {@literal null}.
     * @param annotations    must not be {@literal null}.
     */
    public static void assertQualifiersFor(Class<?>[] parameterTypes, Annotation[][] annotations) {
        Set<String> values = new HashSet<>();

        for (int i = 0; i < annotations.length; i++) {

            if (CursorPageable.class.equals(parameterTypes[i])) {

                Qualifier qualifier = findAnnotation(annotations[i]);

                if (null == qualifier) {
                    throw new IllegalStateException(
                        "Ambiguous Pageable arguments in handler method. If you use multiple parameters of type " +
                            "Pageable you need to qualify them with @Qualifier");
                }

                if (values.contains(qualifier.value())) {
                    throw new IllegalStateException("Values of the user Qualifiers must be unique!");
                }

                values.add(qualifier.value());
            }
        }
    }

    /**
     * Returns a {@link Qualifier} annotation from the given array of {@link Annotation}s. Returns {@literal null} if
     * the array does not contain a {@link Qualifier} annotation.
     *
     * @param annotations must not be {@literal null}.
     * @return
     */
    @Nullable
    private static Qualifier findAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Qualifier) {
                return (Qualifier) annotation;
            }
        }

        return null;
    }
}
