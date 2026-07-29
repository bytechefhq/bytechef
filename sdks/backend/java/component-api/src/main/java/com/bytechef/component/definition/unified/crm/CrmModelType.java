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

package com.bytechef.component.definition.unified.crm;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the CRM (Customer Relationship Management) category of the unified API,
 * covering the sales and relationship entities that CRM providers have in common.
 *
 * @author Ivica Cardic
 */
public enum CrmModelType implements UnifiedApiDefinition.ModelType {

    /** A company or organization tracked in the CRM. */
    ACCOUNT,
    /** An individual person associated with an account. */
    CONTACT,
    /** A recorded interaction, such as a call or email, with a contact or account. */
    ENGAGEMENT,
    /** A prospective, not-yet-qualified sales contact. */
    LEAD,
    /** A free-text note attached to another CRM record. */
    NOTE,
    /** A potential sale or deal in the sales pipeline. */
    OPPORTUNITY,
    /** A to-do item or follow-up action. */
    TASK,
    /** A user of the CRM, such as a sales representative. */
    USER
}
