/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.ObjectMapper;

/**
 * Answers a REST request that failed in a remote call with the remote description as the problem detail: an invalid
 * argument becomes 400 and a missing resource 404, while everything else, a remote denial included, becomes 500 just as
 * the same failure does in the monolith.
 *
 * @version ee
 */
public class GrpcStatusHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public GrpcStatusHandlerExceptionResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        HttpStatus httpStatus = toHttpStatus(status.getCode());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            httpStatus, status.getDescription() == null ? httpStatus.getReasonPhrase() : status.getDescription());

        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));

        httpServletResponse.setStatus(httpStatus.value());
        httpServletResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        try {
            objectMapper.writeValue(httpServletResponse.getOutputStream(), problemDetail);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        return new ModelAndView();
    }

    private static HttpStatus toHttpStatus(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
