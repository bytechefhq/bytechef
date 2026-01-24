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

import com.bytechef.automation.data.table.configuration.domain.DataTable;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Ivica Cardic
 */
public interface DataTableRepository extends CrudRepository<DataTable, Long> {

    Optional<DataTable> findByName(String name);

    long deleteByName(String name);
}
