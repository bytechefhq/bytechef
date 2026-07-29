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

package com.bytechef.component.definition;

/**
 * Represents a category used to classify components, pairing a machine-readable name with a human-readable label. A set
 * of predefined categories is provided as constants.
 *
 * @param name  the machine-readable identifier of the category
 * @param label the human-readable label of the category displayed in the user interface
 * @author Monika Domiter
 */
public record ComponentCategory(String name, String label) {

    /**
     * Creates a category whose label is the same as its name.
     *
     * @param name the machine-readable identifier of the category, also used as its label
     */
    public ComponentCategory(String name) {
        this(name, name);
    }

    public static final ComponentCategory ACCOUNTING = new ComponentCategory("accounting", "Accounting");
    public static final ComponentCategory ADVERTISING = new ComponentCategory("advertising", "Advertising");
    public static final ComponentCategory ANALYTICS = new ComponentCategory("analytics", "Analytics");
    public static final ComponentCategory ARTIFICIAL_INTELLIGENCE = new ComponentCategory(
        "artificial-intelligence", "Artificial Intelligence");
    public static final ComponentCategory ATS = new ComponentCategory("ats", "Applicant Tracking System");
    public static final ComponentCategory CALENDARS_AND_SCHEDULING = new ComponentCategory(
        "calendars-and-scheduling", "Calendars and Scheduling");
    public static final ComponentCategory COMMUNICATION = new ComponentCategory("communication", "Communication");
    public static final ComponentCategory CRM = new ComponentCategory("crm", "CRM");
    public static final ComponentCategory CUSTOMER_SUPPORT = new ComponentCategory(
        "customer-support", "Customer Support");
    public static final ComponentCategory DEVELOPER_TOOLS = new ComponentCategory("developer-tools", "Developer Tools");
    public static final ComponentCategory E_COMMERCE = new ComponentCategory("e-commerce", "E-commerce");
    public static final ComponentCategory FILE_STORAGE = new ComponentCategory("file-storage", "File Storage");
    public static final ComponentCategory HELPERS = new ComponentCategory("helpers", "Helpers");
    public static final ComponentCategory HRIS = new ComponentCategory("hris", "Human Resources Information System");
    public static final ComponentCategory MARKETING_AUTOMATION = new ComponentCategory(
        "marketing-automation", "Marketing Automation");
    public static final ComponentCategory PAYMENT_PROCESSING = new ComponentCategory(
        "payment-processing", "Payment Processing");
    public static final ComponentCategory PRODUCTIVITY_AND_COLLABORATION = new ComponentCategory(
        "productivity-and-collaboration", "Productivity and Collaboration");
    public static final ComponentCategory PROJECT_MANAGEMENT = new ComponentCategory(
        "project-management", "Project Management");
    public static final ComponentCategory SOCIAL_MEDIA = new ComponentCategory("social-media", "Social Media");
    public static final ComponentCategory SURVEYS_AND_FEEDBACK = new ComponentCategory(
        "surveys-and-feedback", "Surveys and Feedback");

    /**
     * Returns the machine-readable identifier of this category.
     *
     * @return the category name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.valueOf(name);
    }
}
