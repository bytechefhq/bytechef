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

package com.bytechef.platform.data.table.execution.domain;

import org.jspecify.annotations.Nullable;

/**
 * A requested change to a row's external id. The patch itself being null means "leave it alone"; a patch holding null
 * means "clear it". Two nulls with two meanings is why this is a record and not a plain nullable string.
 *
 * @author Ivica Cardic
 */
public record ExternalIdPatch(@Nullable String externalId) {
}
