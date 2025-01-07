/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.trigger.handler;

import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.API_PLATFORM;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.NEW_API_REQUEST;

import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.workflow.worker.trigger.handler.AbstractTriggerHandler;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(API_PLATFORM + "/v1/" + NEW_API_REQUEST)
public class ApiPlatformNewApiRequestTriggerHandler extends AbstractTriggerHandler {

    public ApiPlatformNewApiRequestTriggerHandler(TriggerDefinitionFacade triggerDefinitionFacade) {
        super(API_PLATFORM, 1, NEW_API_REQUEST, triggerDefinitionFacade);
    }
}
