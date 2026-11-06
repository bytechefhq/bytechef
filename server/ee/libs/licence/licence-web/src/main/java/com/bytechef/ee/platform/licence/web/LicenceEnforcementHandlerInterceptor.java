/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.licence.LicenceManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI2", "XSS_SERVLET"
})
public class LicenceEnforcementHandlerInterceptor implements HandlerInterceptor {

    private final LicenceManager licenceManager;

    @SuppressFBWarnings("EI2")
    public LicenceEnforcementHandlerInterceptor(LicenceManager licenceManager) {
        this.licenceManager = licenceManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean eeEndpoint = handlerMethod.getBeanType()
            .isAnnotationPresent(ConditionalOnEEVersion.class) ||
            handlerMethod.hasMethodAnnotation(ConditionalOnEEVersion.class);

        if (!eeEndpoint) {
            return true;
        }

        if (licenceManager.getStatus()
            .isActive()) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
        response.setContentType("application/json");

        String body = """
            {"message":"A valid Enterprise licence is required","errorKey":"LICENCE_REQUIRED","status":"%s"}"""
            .formatted(licenceManager.getStatus()
                .name());

        response.getWriter()
            .write(body);

        return false;
    }
}
