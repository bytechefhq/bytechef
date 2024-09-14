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

package com.bytechef.component.myob;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.myob.action.MyobCreateCustomerAction;
import com.bytechef.component.myob.action.MyobCreateCustomerPaymentAction;
import com.bytechef.component.myob.action.MyobCreateSupplierAction;
import com.bytechef.component.myob.action.MyobCreateSupplierPaymentAction;
import com.bytechef.component.myob.connection.MyobConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class MyobComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("myob")
        .title("Myob")
        .description(
            "MYOB is an accounting software that helps businesses manage their finances, invoicing, and payroll.")
        .customAction(true)
        .icon("path:assets/myob.svg")
        .categories(ComponentCategory.ACCOUNTING)
        .connection(MyobConnection.CONNECTION_DEFINITION)
        .actions(
            MyobCreateCustomerAction.ACTION_DEFINITION,
            MyobCreateCustomerPaymentAction.ACTION_DEFINITION,
            MyobCreateSupplierAction.ACTION_DEFINITION,
            MyobCreateSupplierPaymentAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
