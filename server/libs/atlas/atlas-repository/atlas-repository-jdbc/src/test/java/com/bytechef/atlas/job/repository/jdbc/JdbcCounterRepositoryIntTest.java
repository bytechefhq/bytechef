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

import com.bytechef.atlas.domain.Counter;
import com.bytechef.atlas.job.repository.jdbc.config.WorkflowRepositoryIntTestConfiguration;
import com.bytechef.atlas.repository.CounterRepository;
import com.bytechef.test.extension.PostgresTestContainerExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@ExtendWith(PostgresTestContainerExtension.class)
@SpringBootTest(
        classes = WorkflowRepositoryIntTestConfiguration.class,
        properties = "bytechef.workflow.persistence.provider=jdbc")
public class JdbcCounterRepositoryIntTest {

    @Autowired
    private CounterRepository counterRepository;

    @Test
    public void testFindValueById() {
        Counter counter = counterRepository.save(getCounter());

        Long value = counterRepository.findValueByIdForUpdate(counter.getId());

        Assertions.assertEquals(counter.getValue(), value);
    }

    @Test
    public void testUpdate() {
        Counter counter = counterRepository.save(getCounter());

        Long value = counterRepository.findValueByIdForUpdate(counter.getId());

        Assertions.assertEquals(counter.getValue(), value);

        counterRepository.update(counter.getId(), 5);

        value = counterRepository.findValueByIdForUpdate(counter.getId());

        Assertions.assertEquals(5, value);
    }

    @SuppressFBWarnings("DMI")
    private Counter getCounter() {
        Counter counter = new Counter();

        counter.setId(new Random().nextLong() + "");
        counter.setNew(true);
        counter.setValue(3L);

        return counter;
    }
}
