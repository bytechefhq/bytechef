/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.proxy.BodyEncoding;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.ee.automation.app.proxy.ProxyResponse;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppExecutionRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AppExecutionRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppProxyApiController.class)
@WithMockUser
class AppProxyApiControllerIntTest {

    private static final String PROXY_URL = "/api/app/v1/connections/crm/proxy";

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testProxyMapsTheRequestAndTheResponse() throws Exception {
        when(appExecutionFacade.proxy(eq("crm"), any(ProxyRequest.class))).thenReturn(
            new ProxyResponse(
                201, Map.of("Content-Type", List.of("application/json")), "{\"id\":1}", BodyEncoding.UTF8));

        mockMvc.perform(
            post(PROXY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "method": "POST",
                          "path": "/items",
                          "query": {"q": ["a", "b"]},
                          "headers": {"X-Trace": ["t1"]},
                          "body": "{\\"name\\":\\"x\\"}",
                          "bodyEncoding": "UTF8",
                          "contentType": "application/json"
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.headers['Content-Type'][0]").value("application/json"))
            .andExpect(jsonPath("$.body").value("{\"id\":1}"))
            .andExpect(jsonPath("$.bodyEncoding").value("UTF8"));

        ArgumentCaptor<ProxyRequest> captor = ArgumentCaptor.forClass(ProxyRequest.class);

        verify(appExecutionFacade).proxy(eq("crm"), captor.capture());

        ProxyRequest request = captor.getValue();

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/items");
        assertThat(request.query()).isEqualTo(Map.of("q", List.of("a", "b")));
        assertThat(request.headers()).isEqualTo(Map.of("X-Trace", List.of("t1")));
        assertThat(request.body()).isEqualTo("{\"name\":\"x\"}");
        assertThat(request.bodyEncoding()).isEqualTo(BodyEncoding.UTF8);
        assertThat(request.contentType()).isEqualTo("application/json");
    }

    @Test
    void testProxyDefaultsToUtf8AndNoQueryHeadersOrBody() throws Exception {
        when(appExecutionFacade.proxy(eq("crm"), any(ProxyRequest.class))).thenReturn(
            new ProxyResponse(204, Map.of(), null, BodyEncoding.UTF8));

        mockMvc.perform(
            post(PROXY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"GET\",\"path\":\"/items\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(204))
            .andExpect(jsonPath("$.body").value((Object) null))
            .andExpect(jsonPath("$.body").hasJsonPath());

        ArgumentCaptor<ProxyRequest> captor = ArgumentCaptor.forClass(ProxyRequest.class);

        verify(appExecutionFacade).proxy(eq("crm"), captor.capture());

        ProxyRequest request = captor.getValue();

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.query()).isEmpty();
        assertThat(request.headers()).isEmpty();
        assertThat(request.body()).isNull();
        assertThat(request.bodyEncoding()).isEqualTo(BodyEncoding.UTF8);
        assertThat(request.contentType()).isNull();
    }

    @Test
    void testProxyCarriesABase64Body() throws Exception {
        when(appExecutionFacade.proxy(eq("crm"), any(ProxyRequest.class))).thenReturn(
            new ProxyResponse(200, Map.of(), "AAEC", BodyEncoding.BASE64));

        mockMvc.perform(
            post(PROXY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"method\":\"PUT\",\"path\":\"/blob\",\"body\":\"AAEC\",\"bodyEncoding\":\"BASE64\","
                        + "\"contentType\":\"application/octet-stream\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.body").value("AAEC"))
            .andExpect(jsonPath("$.bodyEncoding").value("BASE64"));

        ArgumentCaptor<ProxyRequest> captor = ArgumentCaptor.forClass(ProxyRequest.class);

        verify(appExecutionFacade).proxy(eq("crm"), captor.capture());

        assertThat(captor.getValue()
            .bodyEncoding()).isEqualTo(BodyEncoding.BASE64);
        assertThat(captor.getValue()
            .body()).isEqualTo("AAEC");
    }

    @Test
    void testProxyNotAllowedIsForbidden() throws Exception {
        assertError(AppErrorType.APP_OPERATION_NOT_ALLOWED, "APP_OPERATION_NOT_ALLOWED").andExpect(
            status().isForbidden());
    }

    @Test
    void testProxyPathRejectedIsBadRequest() throws Exception {
        assertError(AppErrorType.APP_PROXY_PATH_REJECTED, "APP_PROXY_PATH_REJECTED").andExpect(
            status().isBadRequest());
    }

    @Test
    void testProxyInvalidRequestIsBadRequest() throws Exception {
        assertError(AppErrorType.APP_PROXY_REQUEST_INVALID, "APP_PROXY_REQUEST_INVALID").andExpect(
            status().isBadRequest());
    }

    @Test
    void testProxyRequestTooLargeIsContentTooLarge() throws Exception {
        assertError(AppErrorType.APP_PROXY_REQUEST_TOO_LARGE, "APP_PROXY_REQUEST_TOO_LARGE").andExpect(
            status().isContentTooLarge());
    }

    @Test
    void testProxyResponseTooLargeIsBadGateway() throws Exception {
        assertError(AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE, "APP_PROXY_RESPONSE_TOO_LARGE").andExpect(
            status().isBadGateway());
    }

    @Test
    void testProxyUpstreamUnavailableIsBadGateway() throws Exception {
        assertError(AppErrorType.APP_PROXY_UPSTREAM_UNAVAILABLE, "APP_PROXY_UPSTREAM_UNAVAILABLE").andExpect(
            status().isBadGateway());
    }

    @Test
    void testProxyUnavailableConnectionIsConflict() throws Exception {
        assertError(AppErrorType.APP_CONNECTION_UNAVAILABLE, "APP_CONNECTION_UNAVAILABLE").andExpect(
            status().isConflict());
    }

    @Test
    void testProxyUnsupportedIsConflict() throws Exception {
        assertError(AppErrorType.APP_PROXY_UNSUPPORTED, "APP_PROXY_UNSUPPORTED").andExpect(status().isConflict());
    }

    @Test
    void testProxyTimeoutIsGatewayTimeout() throws Exception {
        assertError(AppErrorType.APP_EXECUTION_TIMEOUT, "APP_EXECUTION_TIMEOUT").andExpect(
            status().isGatewayTimeout());
    }

    @Test
    void testProxyUnknownBindingIsNotFound() throws Exception {
        assertError(AppErrorType.APP_CONNECTION_NOT_FOUND, "APP_CONNECTION_NOT_FOUND").andExpect(
            status().isNotFound());
    }

    private ResultActions assertError(AppErrorType errorType, String code) throws Exception {
        when(appExecutionFacade.proxy(eq("crm"), any(ProxyRequest.class)))
            .thenThrow(new ConfigurationException("fixed message", errorType));

        return mockMvc.perform(
            post(PROXY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"GET\",\"path\":\"/items\"}"))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.message").value("fixed message"));
    }
}
