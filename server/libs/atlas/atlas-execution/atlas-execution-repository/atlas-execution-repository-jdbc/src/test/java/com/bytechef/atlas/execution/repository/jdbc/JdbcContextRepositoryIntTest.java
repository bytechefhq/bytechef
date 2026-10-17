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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.repository.jdbc.config.WorkflowExecutionRepositoryIntTestConfiguration;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowExecutionRepositoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcContextRepositoryIntTest {

    @Autowired
    private JdbcContextRepository contextRepository;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @AfterEach
    public void afterEach() {
        contextRepository.deleteAll();
    }

    @Test
    public void testFindByStackId() {
        Context context = new Context(
            1L, Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(1L, Context.Classname.TASK_EXECUTION, Map.of("key", "value")));

        context = contextRepository.save(context);

        Context resultContext = contextRepository
            .findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(1L, Context.Classname.TASK_EXECUTION.ordinal())
            .orElseThrow();

        Assertions.assertEquals(context.getValue(), resultContext.getValue());
    }

    @Test
    public void testDeleteAllByStackIdAndClassnameIdKeepsOtherClassname() {
        contextRepository.save(
            new Context(
                7L, Context.Classname.JOB,
                taskFileStorage.storeContextValue(7L, Context.Classname.JOB, Map.of("job", "value"))));

        Context taskExecutionContext = contextRepository.save(
            new Context(
                7L, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(7L, Context.Classname.TASK_EXECUTION, Map.of("task", "value"))));

        contextRepository.deleteAllByStackIdAndClassnameId(7L, Context.Classname.JOB.ordinal());

        List<Context> jobContexts = contextRepository.findAllByStackIdAndClassnameId(
            7L, Context.Classname.JOB.ordinal());
        List<Context> taskExecutionContexts = contextRepository.findAllByStackIdAndClassnameId(
            7L, Context.Classname.TASK_EXECUTION.ordinal());

        Assertions.assertTrue(jobContexts.isEmpty());
        Assertions.assertEquals(1, taskExecutionContexts.size());
        Assertions.assertEquals(taskExecutionContext.getValue(), taskExecutionContexts.getFirst()
            .getValue());
    }
}
