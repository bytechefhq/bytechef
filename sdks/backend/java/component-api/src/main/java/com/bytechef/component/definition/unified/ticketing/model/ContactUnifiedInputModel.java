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

package com.bytechef.component.definition.unified.ticketing.model;

import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import java.util.Map;

/**
 * Normalized, provider-agnostic input model for a ticketing contact. It carries the contact data supplied to a create
 * or update operation in ByteChef's unified shape, before a {@code ProviderContactMapper} converts it into a
 * provider-native input model.
 *
 * @author Ivica Cardic
 */
public class ContactUnifiedInputModel implements UnifiedInputModel {

    /**
     * Returns the provider-specific custom fields carried alongside the standard unified contact fields.
     *
     * @return a map of custom field names to their values; empty when no custom fields are present
     */
    @Override
    public Map<String, ?> getCustomFields() {
        return Map.of();
    }
}
