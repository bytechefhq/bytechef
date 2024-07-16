/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.xero.trigger;

import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.xero.constant.XeroConstants.ACCPAY;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE_OUTPUT_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.NEW_BILL;
import static com.bytechef.component.xero.constant.XeroConstants.WEBHOOK_KEY_PROPERTY;
import static com.bytechef.component.xero.util.XeroUtils.getCreatedObject;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.xero.util.XeroUtils;

/**
 * @author Monika Domiter
 */
public class XeroNewBillTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_BILL)
        .title("New Bill")
        .description("Trigger off whenever a new bill is added.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(WEBHOOK_KEY_PROPERTY)
        .outputSchema(INVOICE_OUTPUT_PROPERTY)
        .workflowSyncValidation(true)
        .webhookValidate(XeroUtils::webhookValidate)
        .staticWebhookRequest(XeroNewBillTrigger::staticWebhookRequest);

    private XeroNewBillTrigger() {
    }

    protected static Object staticWebhookRequest(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        return getCreatedObject(body, context, INVOICE, ACCPAY);
    }
}
