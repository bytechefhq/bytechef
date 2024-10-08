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

package com.bytechef.component.nutshell.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAIL;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAILS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.IS_PRIMARY;
import static com.bytechef.component.nutshell.constant.NutshellConstants.NAME;
import static com.bytechef.component.nutshell.constant.NutshellConstants.PHONE;
import static com.bytechef.component.nutshell.constant.NutshellConstants.PHONES;
import static com.bytechef.component.nutshell.constant.NutshellConstants.VALUE;
import static com.bytechef.component.nutshell.util.NutshellUtils.createEntityBasedOnType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Monika Kušter
 */
public class NutshellCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates new contact")
        .properties(
            string(NAME)
                .label("Name")
                .description("Full name of the contact.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the contact, which appears under their name.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("Primary email address of the contact.")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("Primary phone number of the contact")
                .controlType(ControlType.PHONE)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("contacts")
                            .items(
                                object()
                                    .properties(
                                        string("id"),
                                        string("type"),
                                        string(NAME),
                                        string(DESCRIPTION),
                                        array(EMAILS)
                                            .items(
                                                object()
                                                    .properties(
                                                        bool(IS_PRIMARY),
                                                        string(NAME),
                                                        string(VALUE))),
                                        array(PHONES)
                                            .items(
                                                object()
                                                    .properties(
                                                        bool(IS_PRIMARY),
                                                        string(NAME),
                                                        string(VALUE))))))))
        .perform(NutshellCreateContactAction::perform);

    private NutshellCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return createEntityBasedOnType(inputParameters, actionContext, false);
    }
}
