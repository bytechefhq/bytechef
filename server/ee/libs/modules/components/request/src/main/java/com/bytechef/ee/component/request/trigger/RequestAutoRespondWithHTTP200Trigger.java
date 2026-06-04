/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.request.trigger;

import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.ee.component.request.util.RequestUtils;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class RequestAutoRespondWithHTTP200Trigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("autoRespondWithHTTP200")
        .title("Auto Respond with HTTP 200 Status")
        .description(
            "The request trigger always replies immediately with an HTTP 200 status code in response to any incoming workflow request request. This guarantees execution of the request trigger, but does not involve any validation of the received request.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output()
        .webhookRequest(RequestUtils::getRequestResult);
}
