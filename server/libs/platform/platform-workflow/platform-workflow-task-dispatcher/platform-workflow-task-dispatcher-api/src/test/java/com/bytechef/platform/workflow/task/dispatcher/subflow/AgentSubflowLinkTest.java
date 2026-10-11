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

package com.bytechef.platform.workflow.task.dispatcher.subflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AgentSubflowLinkTest {

    @Test
    void testMetadataRoundTrip() {
        AgentSubflowLink agentSubflowLink = new AgentSubflowLink(7L, "resume-id");

        assertThat(AgentSubflowLink.fromMetadata(agentSubflowLink.toMetadata())).contains(agentSubflowLink);
    }

    @Test
    void testFromMetadataIsEmptyWhenEitherKeyIsMissing() {
        assertThat(AgentSubflowLink.fromMetadata(Map.of())).isEmpty();
        assertThat(AgentSubflowLink.fromMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_ID, 7L))).isEmpty();
        assertThat(AgentSubflowLink.fromMetadata(Map.of(SubflowRequestConstants.AGENT_JOB_RESUME_ID, "resume-id")))
            .isEmpty();
    }

    @Test
    void testRejectsABlankResumeId() {
        assertThatThrownBy(() -> new AgentSubflowLink(7L, " "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
