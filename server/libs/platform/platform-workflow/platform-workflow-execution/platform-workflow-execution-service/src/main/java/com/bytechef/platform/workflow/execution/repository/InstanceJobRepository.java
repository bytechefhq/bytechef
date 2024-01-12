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

package com.bytechef.platform.workflow.execution.repository;

import com.bytechef.platform.workflow.execution.domain.InstanceJob;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface InstanceJobRepository extends ListCrudRepository<InstanceJob, Long>, CustomInstanceJobRepository {

    int DEFAULT_PAGE_SIZE = 20;

    Optional<InstanceJob> findByJobIdAndType(Long jobId, int type);

    Optional<InstanceJob> findTop1ByInstanceIdAndTypeOrderByJobIdDesc(long instanceId, int type);
}
