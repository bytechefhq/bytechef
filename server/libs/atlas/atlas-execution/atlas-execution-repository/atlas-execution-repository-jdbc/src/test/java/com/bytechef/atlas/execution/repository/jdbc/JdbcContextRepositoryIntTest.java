
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

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.file.storage.WorkflowFileStorage;
import com.bytechef.atlas.execution.repository.jdbc.config.WorkflowExecutionRepositoryIntTestConfiguration;
import java.util.Map;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
    private WorkflowFileStorage workflowFileStorage;

    @AfterEach
    public void afterEach() {
        contextRepository.deleteAll();
    }

    @Test
    public void testFindByStackId() {
        Context context = new Context(
            1L, Context.Classname.TASK_EXECUTION,
            workflowFileStorage.storeContextValue(1L, Context.Classname.TASK_EXECUTION, Map.of("key", "value")));

        context = contextRepository.save(context);

        Context resultContext = contextRepository.findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(
            1L, Context.Classname.TASK_EXECUTION.getId());

        Assertions.assertEquals(context.getValue(), resultContext.getValue());
    }
}
