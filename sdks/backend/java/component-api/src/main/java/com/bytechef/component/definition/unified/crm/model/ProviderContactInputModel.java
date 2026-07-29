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

package com.bytechef.component.definition.unified.crm.model;

import com.bytechef.component.definition.unified.base.model.ProviderInputModel;

/**
 * Marker interface for a provider's native contact input model, i.e. the contact payload shaped exactly as the CRM
 * provider expects it on write operations. Provider-specific implementations carry the concrete fields; contact mappers
 * produce instances of this type from the unified {@link ContactUnifiedInputModel}.
 *
 * @author Ivica Cardic
 */
public interface ProviderContactInputModel extends ProviderInputModel {
}
