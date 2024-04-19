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

package com.bytechef.component.capsule.crm.constant;

import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;

/**
 * @author Monika Domiter
 */
public class CapsuleCRMConstants {

    public static final String ABOUT = "about";
    public static final String ADDRESS = "address";
    public static final String ADDRESSES = "addresses";
    public static final String BASE_URL = "https://api.capsulecrm.com/api/v2";
    public static final String CAPSULE_CRM = "capsuleCRM";
    public static final String CATEGORY = "category";
    public static final String CITY = "city";
    public static final String COLOUR = "colour";
    public static final String COUNTRY = "country";
    public static final String CREATE_CONTACT = "createContact";
    public static final String CREATE_TASK = "createTask";
    public static final String DESCRIPTION = "description";
    public static final String DETAIL = "detail";
    public static final String DUE_ON = "dueOn";
    public static final String EMAIL_ADDRESSES = "emailAddresses";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String NAME = "name";
    public static final String NAME_PROPERTIES = "nameProperties";
    public static final String NUMBER = "number";
    public static final String PERSON = "person";
    public static final String PHONE_NUMBERS = "phoneNumbers";
    public static final String STATE = "state";
    public static final String STREET = "street";
    public static final String TYPE = "type";
    public static final String ZIP = "zip";

    public static final ModifiableStringProperty FIRST_NAME_PROPERTY = string(FIRST_NAME)
        .label("First name")
        .description("The first name of the person.")
        .required(true);

    public static final ModifiableStringProperty LAST_NAME_PROPERTY = string(LAST_NAME)
        .label("Last name")
        .description("The last name of the person.")
        .required(true);

    public static final ModifiableStringProperty NAME_PROPERTY = string(NAME)
        .label("Name")
        .description("The name of the organisation.")
        .required(true);

    private CapsuleCRMConstants() {
    }
}
