
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

package com.bytechef.atlas.facade;

import com.bytechef.atlas.config.WorkflowIntTestConfiguration;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.test.annotation.EmbeddedSql;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

@EmbeddedSql
@SpringBootTest(
    classes = WorkflowIntTestConfiguration.class,
    properties = {
        "bytechef.context-repository.provider=jdbc",
        "bytechef.persistence.provider=jdbc",
        "bytechef.workflow-repository.jdbc.enabled=true"
    })
public class JobFacadeIntTest {

    @Autowired
    private JobFacade jobFacade;

    @Test
    public void testRequiredParameters() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> jobFacade.create(new JobParameters(Collections.emptyMap(), "aGVsbG8x")));
    }
}
