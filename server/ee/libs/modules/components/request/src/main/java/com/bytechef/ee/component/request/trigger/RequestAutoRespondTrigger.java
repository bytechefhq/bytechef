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
import com.bytechef.ee.component.request.util.RequestResponseUtils;
import com.bytechef.ee.component.request.util.RequestUtils;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class RequestAutoRespondTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("autoRespondWithHTTP200")
        .title("Auto Respond")
        .description(
            "The request trigger replies immediately to any incoming workflow request and then runs the workflow. By " +
                "default the reply is an empty HTTP 200 response; it can also be a redirect, JSON, raw text or no " +
                "data. The request is not validated.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(RequestResponseUtils.withResponseProperties())
        .output()
        .webhookRequest(RequestUtils::getRequestResult)
        .webhookResponse(RequestResponseUtils::getWebhookResponse);
}
