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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class CopilotSpringAIAgentTest {

    @Nested
    class ConversationIdTest {

        @Test
        void testConversationIdDerivesAUuidFromTheServerScopeAndThreadId() throws AGUIException {
            State state = new State();

            state.set(CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:000001:user-1:workflow-1");

            assertThat(newAgent().conversationId(newInput(state)))
                .isEqualTo(
                    UUID.nameUUIDFromBytes(
                        "embedded:000001:user-1:workflow-1:thread".getBytes(StandardCharsets.UTF_8))
                        .toString());
        }

        @Test
        void testConversationIdFitsTheChatMemoryColumnForALongScope() throws AGUIException {
            State state = new State();

            state.set(
                CopilotConstants.STATE_CONVERSATION_SCOPE,
                "embedded:000001:1234567890:3971538b-be9a-485d-89bb-fc5eb98f8033");

            assertThat(newAgent().conversationId(newInput(state))).hasSizeLessThanOrEqualTo(36);
        }

        @Test
        void testConversationIdSeparatesScopesThatShareAThreadId() throws AGUIException {
            State victimState = new State();

            victimState.set(CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:000001:victim:workflow-1");

            State attackerState = new State();

            attackerState.set(CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:000001:attacker:workflow-1");

            assertThat(newAgent().conversationId(newInput(attackerState)))
                .isNotEqualTo(newAgent().conversationId(newInput(victimState)));
        }

        @Test
        void testConversationIdFallsBackToTheThreadIdWithoutAScope() throws AGUIException {
            assertThat(newAgent().conversationId(newInput(new State()))).isEqualTo("thread");
        }
    }

    @Nested
    class EnvironmentContextTest {

        @BeforeEach
        @AfterEach
        void clearEnvironmentContext() {
            EnvironmentContext.clear();
        }

        @Test
        void testRunWithEnvironmentBindsEnvironmentFromState() throws AGUIException {
            CopilotSpringAIAgent agent = newAgent();

            State state = new State();

            state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 0L);

            AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

            agent.runWithEnvironment(
                newInput(state), () -> capturedEnvironment.set(EnvironmentContext.getCurrentEnvironment()));

            assertThat(capturedEnvironment.get()).isEqualTo(Environment.DEVELOPMENT);
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        }

        @Test
        void testRunWithEnvironmentRestoresPreviousEnvironment() throws AGUIException {
            CopilotSpringAIAgent agent = newAgent();

            EnvironmentContext.set(Environment.STAGING);

            State state = new State();

            state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 0L);

            agent.runWithEnvironment(newInput(state), () -> {});

            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isEqualTo(Environment.STAGING);
        }

        @Test
        void testRunWithEnvironmentLeavesContextUntouchedWhenStateHasNoEnvironment() throws AGUIException {
            CopilotSpringAIAgent agent = newAgent();

            AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

            agent.runWithEnvironment(
                newInput(new State()), () -> capturedEnvironment.set(EnvironmentContext.fetchCurrentEnvironment()));

            assertThat(capturedEnvironment.get()).isNull();
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        }
    }

    @Nested
    @ExtendWith(ObjectMapperSetupExtension.class)
    class EndsTurnTest {

        @Test
        void testEndsTurnForAResultThatAwaitsTheUser() throws AGUIException {
            assertThat(newAgent().endsTurn(newToolResponse("{\"kind\":\"select-connection\",\"awaitingAnswer\":true}")))
                .isTrue();
        }

        @Test
        void testKeepsTheTurnForAResultThatDoesNotAwaitTheUser() throws AGUIException {
            assertThat(
                newAgent().endsTurn(newToolResponse("{\"kind\":\"select-property-option\",\"awaitingAnswer\":false}")))
                    .isFalse();
            assertThat(newAgent().endsTurn(newToolResponse("{\"error\":\"componentName is required\"}")))
                .isFalse();
        }

        @Test
        void testKeepsTheTurnForAResultThatIsNotJsonButMentionsTheKey() throws AGUIException {
            assertThat(newAgent().endsTurn(newToolResponse("awaitingAnswer")))
                .isFalse();
        }

        private static ToolResponseMessage.ToolResponse newToolResponse(String responseData) {
            return new ToolResponseMessage.ToolResponse("call-1", "selectConnection", responseData);
        }
    }

    @Nested
    class RunErrorMessageTest {

        @Test
        void testRunErrorMessageHidesTheCauseFromAScopedRun() throws AGUIException {
            State state = new State();

            state.set(CopilotConstants.STATE_SCOPED_PROJECT_ID, 1114L);

            String message = newAgent().runErrorMessage(
                newInput(state), new IllegalStateException("relation \"workflow\" does not exist"));

            assertThat(message).isEqualTo("The copilot run failed. Reference: run");
        }

        @Test
        void testRunErrorMessageKeepsTheCauseForAnUnscopedRun() throws AGUIException {
            String message = newAgent().runErrorMessage(
                newInput(new State()), new IllegalStateException("No API key configured"));

            assertThat(message).isEqualTo("No API key configured");
        }

        @Test
        void testRunErrorMessageNeverReturnsNull() throws AGUIException {
            String message = newAgent().runErrorMessage(newInput(new State()), new NullPointerException());

            assertThat(message).isEqualTo("The copilot run failed. Reference: run");
        }
    }

    @Nested
    class TenantContextTest {

        private String defaultTenantId;

        @BeforeEach
        void captureDefaultTenant() {
            defaultTenantId = TenantContext.getCurrentTenantId();
        }

        @AfterEach
        void restoreDefaultTenant() {
            TenantContext.setCurrentTenantId(defaultTenantId);
        }

        @Test
        void testRunWithTenantBindsTenantFromState() throws AGUIException {
            CopilotSpringAIAgent agent = newAgent();

            State state = new State();

            state.set(CopilotConstants.STATE_TENANT_ID, "000001");

            AtomicReference<String> capturedTenantId = new AtomicReference<>();

            agent.runWithTenant(newInput(state), () -> capturedTenantId.set(TenantContext.getCurrentTenantId()));

            assertThat(capturedTenantId.get()).isEqualTo("000001");
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(defaultTenantId);
        }

        @Test
        void testRunWithTenantRestoresPreviousTenantWhenActionThrows() throws AGUIException {
            CopilotSpringAIAgent agent = newAgent();

            TenantContext.setCurrentTenantId("000002");

            State state = new State();

            state.set(CopilotConstants.STATE_TENANT_ID, "000001");

            try {
                agent.runWithTenant(newInput(state), () -> {
                    throw new IllegalStateException("boom");
                });
            } catch (IllegalStateException exception) {
                assertThat(exception).hasMessage("boom");
            }

            assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000002");
        }

        @Test
        void testRunWithTenantLeavesContextUntouchedWhenStateHasNoTenant() throws AGUIException {
            CopilotSpringAIAgent agent = newAgent();

            TenantContext.setCurrentTenantId("000002");

            AtomicReference<String> capturedTenantId = new AtomicReference<>();

            agent.runWithTenant(
                newInput(new State()), () -> capturedTenantId.set(TenantContext.getCurrentTenantId()));

            assertThat(capturedTenantId.get()).isEqualTo("000002");
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000002");
        }
    }

    private static RunAgentInput newInput(State state) {
        return new RunAgentInput("thread", "run", state, List.of(), List.of(), List.of(), null);
    }

    private static CopilotSpringAIAgent newAgent() throws AGUIException {
        return ConverterSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();
    }
}
