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

package com.bytechef.ee.platform.ai.tool.usage.repository;

import com.bytechef.ee.platform.ai.tool.usage.AiToolUsage;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiToolUsage}. Insert-only on the hot path; reads are intended for analytics
 * dashboards that scan the table directly and don't need first-class repository methods.
 *
 * @author Ivica Cardic
 */
public interface AiToolUsageRepository extends CrudRepository<AiToolUsage, Long> {
}
