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

package com.bytechef.component.claude.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Marko Kriskovic
 */
public class ClaudeCodeComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/claude-code_v1.json",
            new ClaudeCodeComponentHandler(
                new ApplicationProperties(), mock(ClusterElementDefinitionService.class)).getDefinition());
    }

    @Test
    void testHandlerIsNotRegisteredWhenClaudeCodeIsDisabled() {
        createApplicationContextRunner()
            .run(context -> assertThat(context).doesNotHaveBean(ClaudeCodeComponentHandler.class));
    }

    @Test
    void testHandlerIsRegisteredWhenClaudeCodeIsEnabled() {
        createApplicationContextRunner()
            .withPropertyValues("bytechef.component.claude-code.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(ClaudeCodeComponentHandler.class));
    }

    private static ApplicationContextRunner createApplicationContextRunner() {
        return new ApplicationContextRunner()
            .withBean(ApplicationProperties.class)
            .withBean(ClusterElementDefinitionService.class, () -> mock(ClusterElementDefinitionService.class))
            .withUserConfiguration(ClaudeCodeComponentHandler.class);
    }
}
