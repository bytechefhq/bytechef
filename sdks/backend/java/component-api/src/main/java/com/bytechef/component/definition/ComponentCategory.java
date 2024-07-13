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

package com.bytechef.component.definition;

/**
 * @author Monika Domiter
 */
public enum ComponentCategory {

    ACCOUNTING("accounting"),
    ADVERTISING("advertising"),
    ANALYTICS("analytics"),
    ARTIFICIAL_INTELLIGENCE("artificial-intelligence"),
    ATS("ats"),
    CALENDARS_AND_SCHEDULING("calendars-and-scheduling"),
    COMMUNICATION("communication"),
    CRM("crm"),
    CUSTOMER_SUPPORT("customer-support"),
    DEVELOPER_TOOLS("developer-tools"),
    E_COMMERCE("e-commerce"),
    FILE_STORAGE("file-storage"),
    HELPERS("helpers"),
    HRIS("hris"),
    MARKETING_AUTOMATION("marketing-automation"),
    PAYMENT_PROCESSING("payment-processing"),
    PRODUCTIVITY_AND_COLLABORATION("productivity-and-collaboration"),
    PROJECT_MANAGEMENT("project-management"),
    SOCIAL_MEDIA("social-media"),
    SURVEYS_AND_FEEDBACK("surveys-and-feedback");

    private final String key;

    ComponentCategory(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return String.valueOf(key);
    }
}
