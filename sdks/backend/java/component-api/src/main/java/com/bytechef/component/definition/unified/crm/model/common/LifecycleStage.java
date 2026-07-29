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

package com.bytechef.component.definition.unified.crm.model.common;

/**
 * Enumerates the lifecycle stages of a CRM contact in ByteChef's unified CRM model, describing where a contact sits in
 * the marketing and sales funnel.
 *
 * @author Ivica Cardic
 */
public enum LifecycleStage {

    /**
     * A contact who has subscribed but has not yet been qualified as a lead.
     */
    SUBSCRIBER,

    /**
     * A contact who has shown interest and is considered a lead.
     */
    LEAD,

    /**
     * A lead qualified by marketing as likely to become a customer.
     */
    MARKETING_QUALIFIED_LEAD,

    /**
     * A lead qualified by sales as ready for direct sales engagement.
     */
    SALES_QUALIFIED_LEAD,

    /**
     * A contact associated with an active sales opportunity.
     */
    OPPORTUNITY,

    /**
     * A contact who has become a paying customer.
     */
    CUSTOMER,

    /**
     * A customer who actively promotes the product or brand.
     */
    EVANGELIST,

    /**
     * Any lifecycle stage that does not fall into the other categories.
     */
    OTHER
}
