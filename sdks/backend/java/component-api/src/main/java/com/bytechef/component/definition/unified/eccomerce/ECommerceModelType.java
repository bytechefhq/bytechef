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

package com.bytechef.component.definition.unified.eccomerce;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the e-commerce category of the unified API, covering the storefront and
 * order-management entities that e-commerce providers have in common.
 *
 * @author Ivica Cardic
 */
public enum ECommerceModelType implements UnifiedApiDefinition.ModelType {

    /** A shopper who places orders in the store. */
    CUSTOMER,
    /** A purchase order placed by a customer. */
    ORDER,
    /** A sellable product in the catalog. */
    PRODUCT,
    /** A specific variant of a product, such as a size or color option. */
    PRODUCT_VARIANT,
    /** The shipment or fulfillment of an order's items. */
    FULFILLMENT
}
