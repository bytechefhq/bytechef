
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

package com.bytechef.atlas.job.repository.jdbc;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.job.repository.jdbc.config.WorkflowRepositoryIntTestConfiguration;
import com.bytechef.atlas.repository.ContextRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = WorkflowRepositoryIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.context-repository.provider=jdbc",
        "bytechef.workflow.persistence.provider=jdbc"
    })
public class JdbcContextRepositoryIntTest {

    @Autowired
    private ContextRepository contextRepository;

    @Test
    public void testFindByStackId() {
        Context context = new Context();

        context.setStackId("1");
        context.setValue(Map.of("key", "value"));

        context = contextRepository.save(context);

        Context resultContext = contextRepository.findTop1ByStackIdOrderByCreatedDateDesc("1");

        Assertions.assertEquals(context.getValue(), resultContext.getValue());
    }
}
