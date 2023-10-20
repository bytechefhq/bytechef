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

package com.integri.atlas.task.auth.service;

import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.auth.SimpleTaskAuth;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class TaskAuthService {

    private final TaskAuthRepository taskAuthRepository;

    public TaskAuthService(TaskAuthRepository taskAuthRepository) {
        this.taskAuthRepository = taskAuthRepository;
    }

    public TaskAuth create(String name, String type, Map<String, Object> properties) {
        SimpleTaskAuth simpleTaskAuth = new SimpleTaskAuth();

        simpleTaskAuth.setCreateTime(new Date());
        simpleTaskAuth.setId(UUIDGenerator.generate());
        simpleTaskAuth.setName(name);
        simpleTaskAuth.setProperties(properties);
        simpleTaskAuth.setType(type);
        simpleTaskAuth.setUpdateTime(new Date());

        taskAuthRepository.create(simpleTaskAuth);

        return simpleTaskAuth;
    }

    public void delete(String id) {
        taskAuthRepository.delete(id);
    }

    @Transactional(readOnly = true)
    public TaskAuth getTaskAuth(String id) {
        return taskAuthRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<TaskAuth> getTaskAuths() {
        return taskAuthRepository.findAll();
    }

    public TaskAuth update(String id, String name) {
        SimpleTaskAuth simpleTaskAuth = new SimpleTaskAuth(taskAuthRepository.findById(id));

        simpleTaskAuth.setName(name);
        simpleTaskAuth.setUpdateTime(new Date());

        return taskAuthRepository.update(simpleTaskAuth);
    }
}
