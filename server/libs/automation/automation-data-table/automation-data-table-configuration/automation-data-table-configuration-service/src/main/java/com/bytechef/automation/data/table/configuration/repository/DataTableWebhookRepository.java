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

package com.bytechef.automation.data.table.configuration.repository;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhook;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Ivica Cardic
 */
public interface DataTableWebhookRepository extends CrudRepository<DataTableWebhook, Long> {
    List<DataTableWebhook> findByDataTableId(Long dataTableId);

    List<DataTableWebhook> findByDataTableIdAndEnvironment(Long dataTableId, int environment);

    List<DataTableWebhook> findByDataTableIdAndType(Long dataTableId, int type);

    List<DataTableWebhook> findByDataTableIdAndTypeAndEnvironment(Long dataTableId, int type, int environment);
}
