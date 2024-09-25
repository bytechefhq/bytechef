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

package com.bytechef.component.definition.unified.accounting.model;

import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Account unified input model.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ContactUnifiedOutputModel extends ContactUnifiedInputModel implements UnifiedOutputModel {

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getRemoteId() {
        return "";
    }

    @Override
    public Map<String, ?> getRemoteData() {
        return Map.of();
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return null;
    }

    @Override
    public LocalDateTime getLastModifiedDate() {
        return null;
    }
}
