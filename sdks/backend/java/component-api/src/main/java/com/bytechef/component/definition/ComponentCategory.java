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
public record ComponentCategory(String key, String label) {

    public ComponentCategory(String key) {
        this(key, key);
    }

    public static final ComponentCategory ACCOUNTING = new ComponentCategory("accounting");
    public static final ComponentCategory ADVERTISING = new ComponentCategory("advertising");
    public static final ComponentCategory ANALYTICS = new ComponentCategory("analytics");
    public static final ComponentCategory ARTIFICIAL_INTELLIGENCE = new ComponentCategory("artificial-intelligence");
    public static final ComponentCategory ATS = new ComponentCategory("ats");
    public static final ComponentCategory CALENDARS_AND_SCHEDULING = new ComponentCategory("calendars-and-scheduling");
    public static final ComponentCategory COMMUNICATION = new ComponentCategory("communication");
    public static final ComponentCategory CRM = new ComponentCategory("crm");
    public static final ComponentCategory CUSTOMER_SUPPORT = new ComponentCategory("customer-support");
    public static final ComponentCategory DEVELOPER_TOOLS = new ComponentCategory("developer-tools");
    public static final ComponentCategory E_COMMERCE = new ComponentCategory("e-commerce");
    public static final ComponentCategory FILE_STORAGE = new ComponentCategory("file-storage");
    public static final ComponentCategory HELPERS = new ComponentCategory("helpers");
    public static final ComponentCategory HRIS = new ComponentCategory("hris");
    public static final ComponentCategory MARKETING_AUTOMATION = new ComponentCategory("marketing-automation");
    public static final ComponentCategory PAYMENT_PROCESSING = new ComponentCategory("payment-processing");
    public static final ComponentCategory PRODUCTIVITY_AND_COLLABORATION = new ComponentCategory(
        "productivity-and-collaboration");
    public static final ComponentCategory PROJECT_MANAGEMENT = new ComponentCategory("project-management");
    public static final ComponentCategory SOCIAL_MEDIA = new ComponentCategory("social-media");
    public static final ComponentCategory SURVEYS_AND_FEEDBACK = new ComponentCategory("surveys-and-feedback");

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return String.valueOf(key);
    }
}
