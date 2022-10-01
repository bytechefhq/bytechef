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

package com.bytechef.task.dispatcher.subflow;

import com.bytechef.hermes.test.definition.DefinitionAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class SubflowTaskDispatcherDefinitionFactoryTest {

    @Test
    public void testGetTaskDispatcherDefinition() {
        DefinitionAssert.assertEquals(
                "definition/subflow_v1.json", new SubflowTaskDispatcherDefinitionFactory().getDefinition());
    }
}
