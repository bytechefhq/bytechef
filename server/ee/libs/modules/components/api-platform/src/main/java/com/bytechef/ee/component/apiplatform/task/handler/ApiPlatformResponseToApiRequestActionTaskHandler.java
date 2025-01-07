/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.task.handler;

import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.API_PLATFORM;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE_TO_API_REQUEST;

import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(API_PLATFORM + "/v1/" + RESPONSE_TO_API_REQUEST)
public class ApiPlatformResponseToApiRequestActionTaskHandler extends AbstractTaskHandler {

    public ApiPlatformResponseToApiRequestActionTaskHandler(ActionDefinitionFacade actionDefinitionFacade) {
        super(API_PLATFORM, 1, RESPONSE_TO_API_REQUEST, actionDefinitionFacade);
    }
}
