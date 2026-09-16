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

package com.bytechef.platform.data.table.domain;

/**
 * What one resolution settled: the registry row that won, and the physical table it occupies.
 *
 * <p>
 * The two travel together because a caller needs both and must not fetch them separately. The registry id is what
 * identifies the table to tags, webhooks and metadata lookups; the {@link DataTableRef} is what every statement reaches
 * through. Resolved apart, they could describe two different tables -- which is the whole failure this type exists to
 * make impossible.
 *
 * @author Ivica Cardic
 */
public record DataTableResolution(long dataTableId, DataTableRef dataTableRef) {
}
