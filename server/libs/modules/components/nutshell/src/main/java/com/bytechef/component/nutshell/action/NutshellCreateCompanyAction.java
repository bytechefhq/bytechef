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

package com.bytechef.component.nutshell.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAIL;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAILS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.ID;
import static com.bytechef.component.nutshell.constant.NutshellConstants.IS_PRIMARY;
import static com.bytechef.component.nutshell.constant.NutshellConstants.NAME;
import static com.bytechef.component.nutshell.constant.NutshellConstants.PHONE;
import static com.bytechef.component.nutshell.constant.NutshellConstants.PHONES;
import static com.bytechef.component.nutshell.constant.NutshellConstants.VALUE;
import static com.bytechef.component.nutshell.util.NutshellUtils.createEntityBasedOnType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Kalaiyarasan Raja
 */
public class NutshellCreateCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCompany")
        .title("Create Company")
        .description("Creates new company")
        .properties(
            string(NAME)
                .label("Name")
                .description("Full name of the company.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Detailed Description of the company.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("Primary email address of the company.")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("Primary phone number of the company")
                .controlType(ControlType.PHONE)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("accounts")
                            .items(
                                object()
                                    .properties(
                                        string(ID)
                                            .description("ID of the company."),
                                        string("type")
                                            .description("The type of this entity, e.g. 'contacts', 'leads'."),
                                        string(NAME)
                                            .description("The company full name."),
                                        string(DESCRIPTION)
                                            .description(
                                                "A brief explanation of this company which appears under their name."),
                                        array(EMAILS)
                                            .description("All email addresses associated with a company.")
                                            .items(
                                                object()
                                                    .properties(
                                                        bool(IS_PRIMARY)
                                                            .description(
                                                                "Indicates if this is the primary email address."),
                                                        string(NAME)
                                                            .description(
                                                                "A label for the email address, e.g. 'work', " +
                                                                    "'personal', 'support'."),
                                                        string(VALUE)
                                                            .description("The email address itself."))),
                                        array(PHONES)
                                            .description("All phone numbers associated with a company.")
                                            .items(
                                                object()
                                                    .properties(
                                                        bool(IS_PRIMARY)
                                                            .description(
                                                                "Indicates if this is the primary phone number."),
                                                        string(NAME)
                                                            .description(
                                                                "A label for the phone number, e.g. 'work', " +
                                                                    "'home', 'mobile'."),
                                                        object(VALUE)
                                                            .properties(
                                                                string("countryCode")
                                                                    .description(
                                                                        "Phone number prefix for calling individuals " +
                                                                            "in other countries."),
                                                                string("number")
                                                                    .description(
                                                                        "The unformatted phone number with only digits."),
                                                                string("extension")
                                                                    .description(
                                                                        "An additional code to reach a specific " +
                                                                            "person or department which share a " +
                                                                            "number."),
                                                                string("numberFormatted")
                                                                    .description(
                                                                        "The phone number formatted for human " +
                                                                            "readability."),
                                                                string("E164")
                                                                    .description(
                                                                        "The phone number formatted for " +
                                                                            "international use; a common programatic " +
                                                                            "standard for working with phone numbers."),
                                                                string("countryCodeAndNumber")
                                                                    .description(
                                                                        "The phone number formatted for human " +
                                                                            "readability with the country code.")))))))))
        .perform(NutshellCreateCompanyAction::perform);

    private NutshellCreateCompanyAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return createEntityBasedOnType(inputParameters, actionContext, true);
    }
}
