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

package com.bytechef.component.definition.unified.accounting.model;

import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Represents the normalized, provider-agnostic output shape for an accounting contact. In addition to the contact
 * attributes inherited from {@link ContactUnifiedInputModel}, it exposes the ByteChef identifier, the remote provider
 * identifier, the raw provider payload, and the created and last-modified timestamps returned when reading contacts
 * from a provider.
 *
 * @author Ivica Cardic
 */
public class ContactUnifiedOutputModel extends ContactUnifiedInputModel implements UnifiedOutputModel {

    /**
     * Returns the ByteChef unified identifier of the contact.
     *
     * @return the unified contact identifier
     */
    @Override
    public String getId() {
        return "";
    }

    /**
     * Returns the identifier of the contact in the remote provider system.
     *
     * @return the remote contact identifier
     */
    @Override
    public String getRemoteId() {
        return "";
    }

    /**
     * Returns the raw provider payload for the contact.
     *
     * @return the remote data map
     */
    @Override
    public Map<String, ?> getRemoteData() {
        return Map.of();
    }

    /**
     * Returns the timestamp at which the contact was created.
     *
     * @return the creation timestamp
     */
    @Override
    public OffsetDateTime getCreatedDate() {
        return null;
    }

    /**
     * Returns the timestamp at which the contact was last modified.
     *
     * @return the last-modified timestamp
     */
    @Override
    public OffsetDateTime getLastModifiedDate() {
        return null;
    }
}
