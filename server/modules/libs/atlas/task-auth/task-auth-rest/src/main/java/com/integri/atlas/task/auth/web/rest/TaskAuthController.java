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

package com.integri.atlas.task.auth.web.rest;

import com.integri.atlas.engine.annotation.ConditionalOnCoordinator;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.auth.service.TaskAuthService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class TaskAuthController {

    private final TaskAuthService taskAuthService;

    public TaskAuthController(TaskAuthService taskAuthService) {
        this.taskAuthService = taskAuthService;
    }

    @DeleteMapping(value = "/task-auths/{id}")
    public ResponseEntity<?> deleteTaskAuth(@PathVariable("id") String id) {
        taskAuthService.delete(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/task-auths/{id}")
    public TaskAuth getTaskAuth(@PathVariable("id") String id) {
        return taskAuthService.getTaskAuth(id);
    }

    @GetMapping(value = "/task-auths", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TaskAuth> getTaskAuths() {
        return taskAuthService.getTaskAuths();
    }

    @PostMapping(value = "/task-auths", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskAuth postTaskAuth(@RequestBody TaskAuthCreateDTO taskAuthDTO) {
        return taskAuthService.create(taskAuthDTO.name, taskAuthDTO.type, taskAuthDTO.properties);
    }

    @PutMapping(value = "/task-auths", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskAuth putTaskAuth(@RequestBody TaskAuthMergeDTO taskAuthDTO) {
        return taskAuthService.update(taskAuthDTO.id, taskAuthDTO.name);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void handleIllegalArgumentException(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendError(HttpStatus.BAD_REQUEST.value());
    }

    public record TaskAuthCreateDTO(String name, Map<String, Object> properties, String type) {}

    public record TaskAuthMergeDTO(String id, String name) {}
}
