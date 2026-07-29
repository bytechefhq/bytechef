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

package com.bytechef.component.definition.unified.marketingautomation;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the marketing automation category of the unified API, covering the
 * campaign, messaging, and audience entities that marketing platforms have in common.
 *
 * @author Ivica Cardic
 */
public enum MarketingAutomationModelType implements UnifiedApiDefinition.ModelType {

    /** A single step or action performed within an automation. */
    ACTION,
    /** An automated workflow that executes marketing actions. */
    AUTOMATION,
    /** A marketing campaign. */
    CAMPAIGN,
    /** An individual person in the marketing audience. */
    CONTACT,
    /** A marketing email. */
    EMAIL,
    /** A tracked event, such as an open, click, or conversion. */
    EVENT,
    /** A list or segment of contacts. */
    LIST,
    /** A message sent to a contact through a channel. */
    MESSAGE,
    /** A reusable content template. */
    TEMPLATE,
    /** A user of the marketing automation platform. */
    USER
}
