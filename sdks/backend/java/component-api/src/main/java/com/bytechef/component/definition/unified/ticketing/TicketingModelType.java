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

package com.bytechef.component.definition.unified.ticketing;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the ticketing category of the unified API, covering the support and
 * issue-tracking entities that ticketing platforms have in common.
 *
 * @author Ivica Cardic
 */
public enum TicketingModelType implements UnifiedApiDefinition.ModelType {

    /** A customer account or organization that tickets belong to. */
    ACCOUNT,
    /** A file attached to a ticket or comment. */
    ATTACHMENT,
    /** A collection, project, or board that groups tickets. */
    COLLECTION,
    /** A comment posted on a ticket. */
    COMMENT,
    /** An individual person who raises or is associated with tickets. */
    CONTACT,
    /** A label used to categorize tickets. */
    TAG,
    /** A team responsible for handling tickets. */
    TEAM,
    /** A support ticket or tracked issue. */
    TICKET,
    /** A user of the ticketing system, such as an agent. */
    USER
}
