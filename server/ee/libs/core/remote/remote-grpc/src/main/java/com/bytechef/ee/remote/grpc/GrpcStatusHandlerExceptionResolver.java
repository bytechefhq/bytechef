/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;

/**
 * Answers a REST request that failed in a remote call with the remote description as the problem detail: an invalid
 * argument becomes 400, a missing resource 404 and a remote denial the same 403 ACCESS_DENIED problem detail a local
 * denial gives, while everything else becomes 500 just as the same failure does in the monolith.
 *
 * @version ee
 */
public class GrpcStatusHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GrpcStatusHandlerExceptionResolver.class);

    private static final String ACCESS_DENIED_DETAIL = "Access denied";

    private static final String ACCESS_DENIED_ERROR_CODE = "ACCESS_DENIED";

    private static final AuthenticationTrustResolver AUTHENTICATION_TRUST_RESOLVER =
        new AuthenticationTrustResolverImpl();

    private static final String ERROR_CODE_PROPERTY = "errorCode";

    private final ObjectMapper objectMapper;

    public GrpcStatusHandlerExceptionResolver(ObjectMapper objectMapper) {
        MapperBuilder<?, ?> mapperBuilder = objectMapper.rebuild();

        this.objectMapper = mapperBuilder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public @Nullable ModelAndView resolveException(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, @Nullable Object handler,
        Exception exception) {

        if (!(exception instanceof StatusRuntimeException statusRuntimeException)) {
            return null;
        }

        Status status = statusRuntimeException.getStatus();

        if (status.getCode() == Status.Code.PERMISSION_DENIED) {
            return resolvePermissionDenied(httpServletRequest, httpServletResponse, statusRuntimeException);
        }

        HttpStatus httpStatus = toHttpStatus(status.getCode());

        if (httpStatus.is5xxServerError()) {
            log.error(
                "Remote call for {} failed with {}", httpServletRequest.getRequestURI(), status,
                statusRuntimeException);
        }

        if (httpServletResponse.isCommitted()) {
            return new ModelAndView();
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            httpStatus, status.getDescription() == null ? httpStatus.getReasonPhrase() : status.getDescription());

        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));

        writeProblemDetail(httpServletResponse, problemDetail);

        return new ModelAndView();
    }

    private ModelAndView resolvePermissionDenied(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        StatusRuntimeException statusRuntimeException) {

        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        Status status = statusRuntimeException.getStatus();

        if (AUTHENTICATION_TRUST_RESOLVER.isAnonymous(authentication) ||
            AUTHENTICATION_TRUST_RESOLVER.isRememberMe(authentication)) {

            throw new AccessDeniedException(status.getDescription(), statusRuntimeException);
        }

        log.warn(
            "Remote call for {} was denied: {}", httpServletRequest.getRequestURI(), status.getDescription());

        if (httpServletResponse.isCommitted()) {
            return new ModelAndView();
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ACCESS_DENIED_DETAIL);

        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));
        problemDetail.setProperty(ERROR_CODE_PROPERTY, ACCESS_DENIED_ERROR_CODE);

        writeProblemDetail(httpServletResponse, problemDetail);

        return new ModelAndView();
    }

    void writeProblemDetail(HttpServletResponse httpServletResponse, ProblemDetail problemDetail) {
        httpServletResponse.setStatus(problemDetail.getStatus());
        httpServletResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        try {
            objectMapper.writeValue(httpServletResponse.getOutputStream(), problemDetail);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    private static HttpStatus toHttpStatus(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
