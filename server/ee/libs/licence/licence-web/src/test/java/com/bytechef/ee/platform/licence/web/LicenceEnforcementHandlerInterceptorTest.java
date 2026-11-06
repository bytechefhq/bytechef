/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.licence.LicenceStatus;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class LicenceEnforcementHandlerInterceptorTest {

    private LicenceManager licenceManager;
    private LicenceEnforcementHandlerInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private HandlerMethod eeHandlerMethod;
    private HandlerMethod ceHandlerMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        licenceManager = mock(LicenceManager.class);
        interceptor = new LicenceEnforcementHandlerInterceptor(licenceManager);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        EeController eeController = new EeController();
        Method eeMethod = EeController.class.getMethod("handle");

        eeHandlerMethod = new HandlerMethod(eeController, eeMethod);

        CeController ceController = new CeController();
        Method ceMethod = CeController.class.getMethod("handle");

        ceHandlerMethod = new HandlerMethod(ceController, ceMethod);
    }

    @Test
    void testEeHandlerBlockedWhenLicenceInactive() throws Exception {
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);

        boolean proceed = interceptor.preHandle(request, response, eeHandlerMethod);

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(402);
    }

    @Test
    void testEeHandlerAllowedWhenValid() throws Exception {
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.VALID);

        assertThat(interceptor.preHandle(request, response, eeHandlerMethod)).isTrue();
    }

    @Test
    void testNonEeHandlerAlwaysAllowed() throws Exception {
        assertThat(interceptor.preHandle(request, response, ceHandlerMethod)).isTrue();
        verifyNoInteractions(licenceManager);
    }

    @ConditionalOnEEVersion
    static class EeController {

        public void handle() {
        }
    }

    static class CeController {

        public void handle() {
        }
    }
}
