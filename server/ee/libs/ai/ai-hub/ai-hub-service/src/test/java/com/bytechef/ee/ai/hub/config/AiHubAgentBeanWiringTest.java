/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.server.LocalAgent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Guards the agent-bean wiring contract that keeps the chat REST controllers from crashing at startup.
 *
 * <p>
 * Both {@code CopilotApiController} and {@code AiHubApiController} collect <em>every</em> {@link LocalAgent} bean via
 * {@code List<LocalAgent>} constructor injection and index them with
 * {@code Collectors.toMap(LocalAgent::getAgentId, ...)}. The {@code ai_hub_ask} / {@code ai_hub_build}
 * {@link com.bytechef.ee.ai.hub.agent.AiHubRoutingAgent} routers and the {@code AiHubSpringAIAgent} delegates they wrap
 * deliberately share the same {@code agentId} — the router needs it so the controller dispatches to it, and the
 * delegate needs it so {@code Agent.fromKey(getAgentId())} attributes LLM cost to the right bucket (an unrecognized key
 * collapses to {@code UNKNOWN}). Because both are {@code LocalAgent} subtypes, collection autowiring grabs both and the
 * {@code toMap} throws {@code IllegalStateException: Duplicate key ai_hub_ask}.
 * </p>
 *
 * <p>
 * The fix marks the delegate {@code @Bean}s as {@code defaultCandidate = false} so they stay out of the controllers'
 * {@code List<LocalAgent>} while remaining injectable into the routers via their explicit {@code @Qualifier}. These
 * tests pin both the mechanism (Spring honours {@code defaultCandidate} for collection injection) and the fact that the
 * real config carries the flag.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAgentBeanWiringTest {

    /**
     * Mirrors the controllers' agent indexing so the duplicate-key crash reproduces exactly as it does in production.
     */
    static final class AgentMapHolder {

        private final Map<String, LocalAgent> agentMap;

        AgentMapHolder(List<LocalAgent> localAgents) {
            agentMap = localAgents.stream()
                .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
        }
    }

    /**
     * Minimal concrete {@link LocalAgent} — the wiring tests only ever read {@code getAgentId()}, never run a turn.
     */
    static final class TestLocalAgent extends LocalAgent {

        TestLocalAgent(String agentId) throws AGUIException {
            super(agentId, null, null, "(test)", List.of());
        }

        @Override
        protected void run(RunAgentInput input, AgentSubscriber subscriber) {
            throw new UnsupportedOperationException("test stub never runs a turn");
        }
    }

    @Configuration
    static class CollidingAgentConfiguration {

        @Bean
        LocalAgent springAiDelegate() throws AGUIException {
            return new TestLocalAgent("ai_hub_ask");
        }

        @Bean
        LocalAgent routingAgent() throws AGUIException {
            return new TestLocalAgent("ai_hub_ask");
        }

        @Bean
        AgentMapHolder agentMapHolder(List<LocalAgent> localAgents) {
            return new AgentMapHolder(localAgents);
        }
    }

    @Configuration
    static class NonDefaultCandidateAgentConfiguration {

        @Bean(defaultCandidate = false)
        LocalAgent springAiDelegate() throws AGUIException {
            return new TestLocalAgent("ai_hub_ask");
        }

        @Bean
        LocalAgent routingAgent() throws AGUIException {
            return new TestLocalAgent("ai_hub_ask");
        }

        @Bean
        AgentMapHolder agentMapHolder(List<LocalAgent> localAgents) {
            return new AgentMapHolder(localAgents);
        }
    }

    @Test
    void testTwoAgentsSharingAnIdCrashCollectionAutowiring() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(CollidingAgentConfiguration.class))
            .rootCause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate key ai_hub_ask");
    }

    @Test
    void testNonDefaultCandidateDelegateIsExcludedFromCollectionButResolvableByName() {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(NonDefaultCandidateAgentConfiguration.class)) {

            AgentMapHolder agentMapHolder = context.getBean(AgentMapHolder.class);

            assertThat(agentMapHolder.agentMap).containsOnlyKeys("ai_hub_ask");
            assertThat(agentMapHolder.agentMap.get("ai_hub_ask"))
                .isSameAs(context.getBean("routingAgent", LocalAgent.class));

            // The delegate is still a bean — the router resolves it through its explicit @Qualifier.
            assertThat(context.getBean("springAiDelegate", LocalAgent.class)).isNotNull();
        }
    }

    @Test
    void testSpringAiDelegateBeansAreNonDefaultCandidates() {
        for (String beanMethodName : List.of("aiHubAskSpringAIAgent", "aiHubBuildSpringAIAgent")) {
            Method beanMethod = findBeanMethod(beanMethodName);
            Bean bean = beanMethod.getAnnotation(Bean.class);

            assertThat(bean)
                .as("%s must be a @Bean method", beanMethodName)
                .isNotNull();
            assertThat(bean.defaultCandidate())
                .as(
                    "%s must be defaultCandidate=false so it stays out of the chat controllers' List<LocalAgent>",
                    beanMethodName)
                .isFalse();
        }
    }

    private static Method findBeanMethod(String beanMethodName) {
        for (Method method : AiHubConfiguration.class.getDeclaredMethods()) {
            if (method.getName()
                .equals(beanMethodName)) {

                return method;
            }
        }

        throw new AssertionError("No method named " + beanMethodName + " on AiHubConfiguration");
    }
}
