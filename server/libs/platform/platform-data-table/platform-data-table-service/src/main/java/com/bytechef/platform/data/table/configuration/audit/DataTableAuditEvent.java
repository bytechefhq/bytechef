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

package com.bytechef.platform.data.table.configuration.audit;

/**
 * Audit event types emitted through {@link DataTableAuditPublisher} for user-driven data-table and schema mutations.
 *
 * <p>
 * Agent-driven row mutations are audited separately (through the AI Hub audit publisher); this enum covers only the
 * table/schema operations a user performs against the data-table aggregate. The payload key contract documents the
 * fields each event carries beyond the implicit {@code dataTableId} (which {@link DataTableAuditPublisher} attaches to
 * every event).
 *
 * @author Ivica Cardic
 */
public enum DataTableAuditEvent {

    /**
     * A new data table was created. Payload: {@code name} (the table base name).
     */
    DATA_TABLE_CREATED,

    /**
     * A column was added to an existing data table. Payload: {@code columnName} (the added column name).
     */
    DATA_TABLE_COLUMN_ADDED,

    /**
     * A data table was deleted (the physical table dropped and its registry row removed). Payload: no additional keys
     * required; {@code dataTableId} identifies the now-removed table.
     */
    DATA_TABLE_DELETED
}
