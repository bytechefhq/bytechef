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

package com.bytechef.automation.task.service;

import com.bytechef.automation.task.domain.Task;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface TaskService {

    long countTasks();

    Task create(Task task);

    void delete(long id);

    Optional<Task> fetchTask(String name);

    Task getTask(long id);

    List<Task> getTasks();

    List<Task> getTasks(List<Long> ids);

    Task update(Task task);
}
