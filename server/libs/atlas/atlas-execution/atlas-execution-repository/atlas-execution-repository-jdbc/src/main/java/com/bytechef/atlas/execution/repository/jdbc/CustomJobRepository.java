
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.execution.domain.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface CustomJobRepository {

    long count(String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds);

    List<Job> findAll(String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds);

    Page<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds,
        Pageable pageable);
}
