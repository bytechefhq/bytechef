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

package com.bytechef.platform.data.table.configuration.service;

import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableTagServiceImpl implements DataTableTagService {

    private final DataTableRepository dataTableRepository;

    @SuppressFBWarnings("EI")
    public DataTableTagServiceImpl(DataTableRepository dataTableRepository) {
        this.dataTableRepository = dataTableRepository;
    }

    @Override
    public List<Long> getTagIds(List<Long> dataTableIds) {
        Set<Long> tagIds = new LinkedHashSet<>();

        for (Long dataTableId : dataTableIds) {
            DataTable dataTable = dataTableRepository.findById(dataTableId)
                .orElse(null);

            if (dataTable == null) {
                continue;
            }

            List<Long> dataTableTagIds = dataTable.getTagIds();

            if (dataTableTagIds != null) {
                tagIds.addAll(dataTableTagIds);
            }
        }

        return new ArrayList<>(tagIds);
    }

    @Override
    public Map<Long, List<Long>> getTagIdsByTableId() {
        Map<Long, List<Long>> tagIdsByTableId = new HashMap<>();

        for (DataTable dataTable : dataTableRepository.findAll()) {
            tagIdsByTableId.put(dataTable.getId(), dataTable.getTagIds());
        }

        return tagIdsByTableId;
    }

    @Override
    public void updateTagIds(long tableId, List<Long> tagIds) {
        DataTable dataTable = dataTableRepository.findById(tableId)
            .orElseThrow(() -> new IllegalArgumentException("Data table with id=" + tableId + " not found"));

        dataTable.setTagIds(tagIds);

        dataTableRepository.save(dataTable);
    }
}
