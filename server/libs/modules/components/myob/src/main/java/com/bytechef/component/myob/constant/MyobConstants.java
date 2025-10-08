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

package com.bytechef.component.myob.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.myob.util.MyobUtils;

/**
 * @author Monika Kušter
 */
public class MyobConstants {

    public static final String ACCOUNT = "Account";
    public static final String ADDRESSES = "Addresses";
    public static final String COMPANY_FILE = "companyFile";
    public static final String COMPANY_NAME = "CompanyName";
    public static final String CUSTOMER = "Customer";
    public static final String FIRST_NAME = "FirstName";
    public static final String IS_ACTIVE = "IsActive";
    public static final String IS_INDIVIDUAL = "IsIndividual";
    public static final String LAST_NAME = "LastName";
    public static final String PAY_FROM = "PayFrom";
    public static final String SUPPLIER = "Supplier";
    public static final String UID = "UID";

    public static final ModifiableArrayProperty ADDRESSES_PROPERTY = array(ADDRESSES)
        .label("Addresses")
        .description("List of addresses for the customer contact.")
        .items(
            object()
                .properties(
                    string("Street")
                        .maxLength(255)
                        .required(false),
                    string("City")
                        .maxLength(255)
                        .required(false),
                    string("State")
                        .maxLength(255)
                        .required(false),
                    string("PostCode")
                        .maxLength(11)
                        .required(false),
                    string("Country")
                        .maxLength(255)
                        .required(false),
                    string("Phone1")
                        .label("Phone")
                        .maxLength(21)
                        .required(false),
                    string("Email")
                        .maxLength(255)
                        .required(false),
                    string("Website")
                        .maxLength(255)
                        .required(false)))
        .required(false);

    public static final ModifiableStringProperty COMPANY_FILE_PROPERTY = string(COMPANY_FILE)
        .label("Company File")
        .description("The MYOB company file to use.")
        .options((OptionsFunction<String>) MyobUtils::getCompanyFileOptions)
        .required(true);

    public static final ModifiableStringProperty FIRST_NAME_PROPERTY = string(FIRST_NAME)
        .maxLength(20)
        .label("First Name")
        .description("First name for an individual contact.")
        .displayCondition("%s == true".formatted(IS_INDIVIDUAL))
        .required(true);

    public static final ModifiableStringProperty LAST_NAME_PROPERTY = string(LAST_NAME)
        .maxLength(30)
        .label("Last Name")
        .description("Last name for an individual contact.")
        .displayCondition("%s == true".formatted(IS_INDIVIDUAL))
        .required(true);

    public static final ModifiableStringProperty PAY_FROM_PROPERTY = string(PAY_FROM)
        .label("Pay From")
        .options(
            option(ACCOUNT, ACCOUNT),
            option("Electronic Payments", "ElectronicPayments"))
        .required(true);

    private MyobConstants() {
    }
}
