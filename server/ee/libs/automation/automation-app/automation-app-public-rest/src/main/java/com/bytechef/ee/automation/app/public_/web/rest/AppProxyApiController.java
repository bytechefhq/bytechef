/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.proxy.BodyEncoding;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.ee.automation.app.proxy.ProxyResponse;
import com.bytechef.ee.automation.app.public_.web.rest.model.ProxyRequestModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.ProxyResponseModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The raw HTTP proxy an app calls with its own secret key. {@link AppExecutionFacade} owns the binding checks, the
 * execution and its recording; this controller only maps the request and the response to and from the wire models.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppProxyApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppProxyApiController extends AbstractAppApiController implements AppProxyApi {

    private final AppExecutionFacade appExecutionFacade;

    @SuppressFBWarnings("EI2")
    public AppProxyApiController(AppExecutionFacade appExecutionFacade) {
        this.appExecutionFacade = appExecutionFacade;
    }

    @Override
    public ResponseEntity<ProxyResponseModel> proxy(String bindingKey, ProxyRequestModel proxyRequestModel) {
        return ResponseEntity.ok(toModel(appExecutionFacade.proxy(bindingKey, toRequest(proxyRequestModel))));
    }

    private static ProxyResponseModel toModel(ProxyResponse proxyResponse) {
        return new ProxyResponseModel(
            proxyResponse.status(), proxyResponse.headers(),
            ProxyResponseModel.BodyEncodingEnum.fromValue(
                proxyResponse.bodyEncoding()
                    .name()))
                        .body(proxyResponse.body());
    }

    private static ProxyRequest toRequest(ProxyRequestModel proxyRequestModel) {
        ProxyRequestModel.BodyEncodingEnum bodyEncoding = proxyRequestModel.getBodyEncoding();

        return new ProxyRequest(
            proxyRequestModel.getMethod()
                .getValue(),
            proxyRequestModel.getPath(), proxyRequestModel.getQuery(), proxyRequestModel.getHeaders(),
            proxyRequestModel.getBody()
                .orElse(null),
            bodyEncoding == null ? BodyEncoding.UTF8 : BodyEncoding.valueOf(bodyEncoding.getValue()),
            proxyRequestModel.getContentType());
    }
}
