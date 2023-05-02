
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

package com.bytechef.hermes.repository;

import com.bytechef.hermes.domain.TriggerLifecycle;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Repository
public interface TriggerLifecycleRepository
    extends ListPagingAndSortingRepository<TriggerLifecycle, Long>, ListCrudRepository<TriggerLifecycle, Long> {

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<TriggerLifecycle> findByInstanceIdAndWorkflowExecutionId(long instanceId, String workflowExecutionId);
}
