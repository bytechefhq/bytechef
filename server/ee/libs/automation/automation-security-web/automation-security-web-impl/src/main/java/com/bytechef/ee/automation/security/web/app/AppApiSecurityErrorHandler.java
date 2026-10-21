/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Answers a rejected app key with 401 as a marked {@code {"code", "message"}} error: {@code APP_DISABLED} for a
 * disabled app and {@code UNAUTHORIZED} for any other rejected key. The marker lets a client tell these from a status a
 * workflow chose.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppApiSecurityErrorHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        AuthenticationException authenticationException) throws IOException {

        String code = authenticationException instanceof DisabledException
            ? AppErrorType.nameOf(AppErrorType.APP_DISABLED.getErrorKey()) : "UNAUTHORIZED";

        write(httpServletResponse, HttpStatus.UNAUTHORIZED, code, authenticationException.getMessage());
    }

    @SuppressFBWarnings(value = "XSS_SERVLET", justification = "A JSON body with every value JSON-escaped")
    private static void write(
        HttpServletResponse httpServletResponse, HttpStatus status, @Nullable String code, @Nullable String message)
        throws IOException {

        httpServletResponse.setStatus(status.value());
        httpServletResponse.setHeader(AppErrorType.APP_ERROR_HEADER, "true");
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

        httpServletResponse.getWriter()
            .write("{\"code\":" + toJsonString(code) + ",\"message\":" + toJsonString(message) + "}");
    }

    private static String toJsonString(@Nullable String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder jsonString = new StringBuilder("\"");

        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> jsonString.append("\\\"");
                case '\\' -> jsonString.append("\\\\");
                case '\n' -> jsonString.append("\\n");
                case '\r' -> jsonString.append("\\r");
                case '\t' -> jsonString.append("\\t");
                default -> {
                    if (character < 0x20) {
                        jsonString.append(String.format("\\u%04x", (int) character));
                    } else {
                        jsonString.append(character);
                    }
                }
            }
        }

        return jsonString.append('"')
            .toString();
    }
}
