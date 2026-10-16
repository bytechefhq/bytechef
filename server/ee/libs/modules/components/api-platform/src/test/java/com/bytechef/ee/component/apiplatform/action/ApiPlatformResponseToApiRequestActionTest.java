/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.action;

import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.FORBIDDEN;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INTERNAL_ERROR;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INVALID_INPUT;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE_TYPE;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.ee.component.apiplatform.constant.ResponseType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ApiPlatformResponseToApiRequestActionTest {

    private final ApiPlatformResponseToApiRequestAction apiPlatformResponseToApiRequestAction =
        new ApiPlatformResponseToApiRequestAction(null);

    @Test
    void testPerformSuccessReturnsOk() {
        assertStatusCode(ResponseType.SUCCESS, SUCCESS, HttpStatus.OK);
    }

    @Test
    void testPerformInvalidInputReturnsBadRequest() {
        assertStatusCode(ResponseType.INVALID_INPUT, INVALID_INPUT, HttpStatus.BAD_REQUEST);
    }

    @Test
    void testPerformInternalErrorReturnsInternalServerError() {
        assertStatusCode(ResponseType.INTERNAL_ERROR, INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testPerformForbiddenReturnsForbidden() {
        assertStatusCode(ResponseType.FORBIDDEN, FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    private void assertStatusCode(ResponseType responseType, String responseKey, HttpStatus expectedStatus) {
        Map<String, Object> body = Map.of("message", "value");

        Parameters inputParameters = MockParametersFactory.create(
            Map.of(RESPONSE_TYPE, responseType.name(), RESPONSE, Map.of(responseKey, body)));

        WebhookResponse webhookResponse = apiPlatformResponseToApiRequestAction.perform(inputParameters, null, null);

        assertThat(webhookResponse.getStatusCode()).isEqualTo(expectedStatus.getValue());
        assertThat(webhookResponse.getBody()).isEqualTo(body);
    }
}
