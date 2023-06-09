
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

package com.bytechef.helios.configuration.repository;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectInstanceWorkflowConnectionRepository
    extends org.springframework.data.repository.Repository<ProjectInstanceWorkflowConnection, Long> {

    Optional<ProjectInstanceWorkflowConnection> findByKeyAndTaskName(String key, String taskName);
}
