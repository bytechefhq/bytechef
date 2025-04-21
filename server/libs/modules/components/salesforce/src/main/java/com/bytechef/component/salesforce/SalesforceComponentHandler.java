/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.salesforce;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.salesforce.action.SalesforceCreateRecordAction;
import com.bytechef.component.salesforce.action.SalesforceDeleteRecordAction;
import com.bytechef.component.salesforce.action.SalesforceSOQLQueryAction;
import com.bytechef.component.salesforce.action.SalesforceUpdateRecordAction;
import com.bytechef.component.salesforce.connection.SalesforceConnection;
import com.bytechef.component.salesforce.trigger.SalesforceNewRecordTrigger;
import com.bytechef.component.salesforce.trigger.SalesforceUpdatedRecordTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class SalesforceComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("salesforce")
        .title("Salesforce")
        .description(
            "Salesforce is a cloud-based customer relationship management (CRM) platform that provides tools for " +
                "sales, service, marketing, and analytics to help businesses manage customer interactions and data.")
        .icon("path:assets/salesforce.svg")
        .categories(ComponentCategory.CRM)
        .customAction(true)
        .connection(SalesforceConnection.CONNECTION_DEFINITION)
        .actions(
            SalesforceCreateRecordAction.ACTION_DEFINITION,
            SalesforceDeleteRecordAction.ACTION_DEFINITION,
            SalesforceSOQLQueryAction.ACTION_DEFINITION,
            SalesforceUpdateRecordAction.ACTION_DEFINITION)
        .clusterElements(
            tool(SalesforceCreateRecordAction.ACTION_DEFINITION),
            tool(SalesforceDeleteRecordAction.ACTION_DEFINITION),
            tool(SalesforceSOQLQueryAction.ACTION_DEFINITION),
            tool(SalesforceUpdateRecordAction.ACTION_DEFINITION))
        .triggers(
            SalesforceNewRecordTrigger.TRIGGER_DEFINITION,
            SalesforceUpdatedRecordTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
