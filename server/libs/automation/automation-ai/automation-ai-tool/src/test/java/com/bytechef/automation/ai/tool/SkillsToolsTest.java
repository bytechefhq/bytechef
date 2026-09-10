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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class SkillsToolsTest {

    @Mock
    private AiSkillFacade aiSkillFacade;

    @Test
    void testDeleteAiSkillReturnsConfirmationMessage() {
        SkillsTools tools = new SkillsTools(aiSkillFacade);

        String result = tools.deleteAiSkill(5L);

        verify(aiSkillFacade).deleteAiSkill(5L);
        assertThat(result).isNotBlank()
            .contains("5");
    }
}
