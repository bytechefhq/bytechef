/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.server.LocalAgent;
import com.bytechef.ai.copilot.config.InteractivePickerToolCallbacks;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.agent.AiHubChatStreamer;
import com.bytechef.ee.ai.hub.agent.AiHubContinuationStarterImpl;
import com.bytechef.ee.ai.hub.agent.AiHubRoutingAgent;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.WebhookBridgeAgent;
import com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubContinuationStarter;
import com.bytechef.ee.ai.hub.approval.AiHubPinnedToolCallbacks;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalFacade;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalFacadeImpl;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicyImpl;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleFacade;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleFacadeImpl;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleServiceImpl;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalServiceImpl;
import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRepository;
import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRuleRepository;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicyImpl;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactServiceImpl;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatServiceImpl;
import com.bytechef.ee.ai.hub.chat.TitleGenerationService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatArtifactRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.memory.AiHubTurnEndingToolResultAdvisor;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.web.graphql.AiHubChatArtifactGraphQlController;
import com.bytechef.ee.ai.hub.web.graphql.AiHubChatGraphQlController;
import com.bytechef.ee.ai.hub.web.graphql.AiHubToolApprovalGraphQlController;
import com.bytechef.ee.ai.hub.web.graphql.AiHubToolApprovalRuleGraphQlController;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.MemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Starts {@link AiHubConfiguration} in a real application context: the beans exist only behind both toggles, the
 * session memory bean is registered, and a BUILD tool list without {@code buildWorkflow} stops the context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationContextTest {

    @Test
    void testRegistersAgentsSessionMemoryAndPinnedToolsWhenHubAndCopilotAreEnabled() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        contextRunner(fixtures)
            .withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("aiHubSessionMemory");
                assertThat(context).hasSingleBean(AiHubSessionMemory.class);
                assertThat(context.getBeansOfType(LocalAgent.class)).containsOnlyKeys(
                    "aiHubAskSpringAIAgent", "aiHubBuildSpringAIAgent", "aiHubAskRoutingAgent",
                    "aiHubBuildRoutingAgent", "webhookBridgeAgent");
                assertInjectedLocalAgentsAreTheTwoRouters(context);
                assertBothRoutersHoldTheBridge(context);

                AiHubSpringAIAgent askAgent = context.getBean("aiHubAskSpringAIAgent", AiHubSpringAIAgent.class);
                AiHubSpringAIAgent buildAgent = context.getBean("aiHubBuildSpringAIAgent", AiHubSpringAIAgent.class);

                assertThat(askAgent.getAgentId()).isEqualTo("ai_hub_ask");
                assertThat(buildAgent.getAgentId()).isEqualTo("ai_hub_build");

                AiHubPinnedToolCallbacks pinnedToolCallbacks = context.getBean(AiHubPinnedToolCallbacks.class);

                assertThat(pinnedToolCallbacks.pinnedToolCallbacks(Mode.ASK)).isEqualTo(
                    askAgent.pinnedToolCallbacks());
                assertThat(pinnedToolCallbacks.pinnedToolCallbacks(Mode.BUILD)).isEqualTo(
                    buildAgent.pinnedToolCallbacks());
            });
    }

    @Test
    void testStartsTheFullHubBeanGraphWhenHubAndCopilotAreEnabled() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        fullGraphRunner(fixtures)
            .withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(InFlightAiHubRunRegistry.class);
                assertThat(context).hasSingleBean(AiHubChatStreamer.class);
                assertThat(context).hasSingleBean(AiHubContinuationStarterImpl.class);
                assertThat(context).hasSingleBean(AiHubContinuationStarter.class);
                assertThat(context).hasSingleBean(AiHubChatServiceImpl.class);
                assertThat(context).hasSingleBean(AiHubToolApprovalFacade.class);
                assertThat(context).hasSingleBean(AiHubToolApprovalRuleFacade.class);
                assertThat(context).hasSingleBean(AiHubChatGraphQlController.class);
                assertThat(context).hasSingleBean(AiHubChatArtifactGraphQlController.class);
                assertThat(context).hasSingleBean(AiHubToolApprovalGraphQlController.class);
                assertThat(context).hasSingleBean(AiHubToolApprovalRuleGraphQlController.class);
                assertInjectedLocalAgentsAreTheTwoRouters(context);
                assertBothRoutersHoldTheBridge(context);
            });
    }

    @Test
    void testStartsNoHubBeansAndNoControllersWhenCopilotIsDisabled() {
        fullGraphRunner(AiHubAgentFixtures.withBuildWorkflow())
            .withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(InFlightAiHubRunRegistry.class);
                assertThat(context).doesNotHaveBean(AiHubChatServiceImpl.class);
                assertThat(context).doesNotHaveBean(AiHubChatGraphQlController.class);
                assertThat(context).doesNotHaveBean(AiHubToolApprovalGraphQlController.class);
            });
    }

    @Test
    void testRegistersNothingWhenCopilotIsDisabled() {
        contextRunner(AiHubAgentFixtures.withBuildWorkflow())
            .withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("aiHubSessionMemory");
                assertThat(context).doesNotHaveBean(LocalAgent.class);
            });
    }

    @Test
    void testStartupFailsWhenBuildWorkflowIsMissing() {
        contextRunner(new AiHubAgentFixtures(Set.of("importWorkflow")))
            .withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true")
            .run(context -> assertThat(context).getFailure()
                .rootCause()
                .hasMessageContaining("buildWorkflow")
                .hasMessageContaining("bytechef.ai.hub.enabled")
                .hasMessageContaining("bytechef.ai.copilot.enabled"));
    }

    @Test
    void testBothAgentsRegisterTheTurnEndingToolResultAdvisor() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        contextRunner(fixtures)
            .withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();

                for (String agentBeanName : List.of("aiHubAskSpringAIAgent", "aiHubBuildSpringAIAgent")) {
                    AiHubSpringAIAgent agent = context.getBean(agentBeanName, AiHubSpringAIAgent.class);

                    List<?> advisors = (List<?>) ReflectionTestUtils.getField(agent, "advisors");

                    assertThat(advisors).as(agentBeanName)
                        .hasAtLeastOneElementOfType(AiHubTurnEndingToolResultAdvisor.class);
                }
            });
    }

    @Test
    void testSessionMemorySitsInsideTheToolCallingLoop() {
        SessionMemoryAdvisor sessionMemoryAdvisor = AiHubAgentFixtures.withBuildWorkflow().sessionMemory
            .createSessionMemoryAdvisor();

        assertThat(sessionMemoryAdvisor).isInstanceOf(MemoryAdvisor.class);
        assertThat(sessionMemoryAdvisor.getOrder()).isGreaterThan(ToolCallingAdvisor.DEFAULT_ORDER);
    }

    private static void assertInjectedLocalAgentsAreTheTwoRouters(ApplicationContext context) {
        List<LocalAgent> localAgents = context.getBean(LocalAgentsHolder.class).localAgents;

        assertThat(localAgents).extracting(LocalAgent::getAgentId)
            .containsExactlyInAnyOrder("ai_hub_ask", "ai_hub_build");
        assertThat(localAgents).allMatch(AiHubRoutingAgent.class::isInstance);
    }

    private static void assertBothRoutersHoldTheBridge(ApplicationContext context) {
        assertThat(context.getBeansOfType(WebhookBridgeAgent.class)).containsOnlyKeys("webhookBridgeAgent");

        WebhookBridgeAgent webhookBridgeAgent = context.getBean(WebhookBridgeAgent.class);

        for (String routerBeanName : List.of("aiHubAskRoutingAgent", "aiHubBuildRoutingAgent")) {
            AiHubRoutingAgent router = context.getBean(routerBeanName, AiHubRoutingAgent.class);

            assertThat(ReflectionTestUtils.getField(router, "webhookBridgeAgent")).as(routerBeanName)
                .isSameAs(webhookBridgeAgent);
        }
    }

    private static ApplicationContextRunner contextRunner(AiHubAgentFixtures fixtures) {
        return new ApplicationContextRunner()
            .withPropertyValues("bytechef.ai.memory.provider=in_memory")
            .withUserConfiguration(
                AiHubConfiguration.class, WebhookResumeRegistry.class, WorkflowChatGuard.class,
                WorkflowChatMetrics.class)
            .withBean(ChatModel.class, () -> fixtures.chatModel)
            .withBean(ReadProjectTools.class, () -> fixtures.readProjectTools)
            .withBean(ReadProjectWorkflowTools.class, () -> fixtures.readProjectWorkflowTools)
            .withBean(WorkflowExecutionTools.class, () -> fixtures.workflowExecutionTools)
            .withBean(ProjectTools.class, () -> fixtures.projectTools)
            .withBean(ProjectWorkflowLifecycleTools.class, () -> fixtures.projectWorkflowLifecycleTools)
            .withBean(
                DeploymentToolCallbacksFactory.class,
                () -> new DeploymentToolCallbacksFactory(fixtures.projectDeploymentFacade))
            .withBean(InteractivePickerToolCallbacks.class, () -> fixtures.interactivePickerToolCallbacks)
            .withBean(IntelligentToolCatalog.class, () -> fixtures.intelligentToolCatalog)
            .withBean(WorkspaceFacade.class, () -> fixtures.workspaceFacade)
            .withBean(ProjectWorkflowFacade.class, () -> fixtures.projectWorkflowFacade)
            .withBean(AiHubChatArtifactRecorder.class, () -> fixtures.artifactRecorder)
            .withBean(SecurityContextRehydrator.class, () -> fixtures.securityContextRehydrator)
            .withBean(AiHubApprovalGate.class, () -> fixtures.approvalGate)
            .withBean(AiHubToolApprovalPolicy.class, () -> fixtures.policy)
            .withBean(AiHubChatService.class, () -> fixtures.chatService)
            .withBean(AiHubChatAccessPolicy.class, () -> fixtures.chatAccessPolicy)
            .withBean(ProjectDeploymentFacade.class, () -> fixtures.projectDeploymentFacade)
            .withBean(InFlightAiHubRunRegistry.class, () -> fixtures.inFlightRunRegistry)
            .withBean(JsonMapper.class, () -> JsonMapper.builder()
                .build())
            .withBean(CacheManager.class, ConcurrentMapCacheManager::new)
            .withBean(WebhookWorkflowExecutor.class, () -> mock(WebhookWorkflowExecutor.class))
            .withBean(LocalAgentsHolder.class);
    }

    private static ApplicationContextRunner fullGraphRunner(AiHubAgentFixtures fixtures) {
        return new ApplicationContextRunner()
            .withPropertyValues("bytechef.ai.memory.provider=in_memory", "bytechef.edition=ee")
            .withUserConfiguration(
                AiHubConfiguration.class, InFlightAiHubRunRegistry.class, AiHubChatStreamer.class,
                AiHubContinuationStarterImpl.class, AiHubChatServiceImpl.class, AiHubChatArtifactServiceImpl.class,
                AiHubChatAccessPolicyImpl.class, AiHubToolApprovalServiceImpl.class,
                AiHubToolApprovalRuleServiceImpl.class, AiHubToolApprovalPolicyImpl.class,
                AiHubToolApprovalFacadeImpl.class, AiHubToolApprovalRuleFacadeImpl.class, AiHubApprovalGate.class,
                TitleGenerationService.class, AiHubChatGraphQlController.class,
                AiHubChatArtifactGraphQlController.class, AiHubToolApprovalGraphQlController.class,
                AiHubToolApprovalRuleGraphQlController.class, WebhookResumeRegistry.class, WorkflowChatGuard.class,
                WorkflowChatMetrics.class)
            .withBean(ChatModel.class, () -> fixtures.chatModel)
            .withBean(ReadProjectTools.class, () -> fixtures.readProjectTools)
            .withBean(ReadProjectWorkflowTools.class, () -> fixtures.readProjectWorkflowTools)
            .withBean(WorkflowExecutionTools.class, () -> fixtures.workflowExecutionTools)
            .withBean(ProjectTools.class, () -> fixtures.projectTools)
            .withBean(ProjectWorkflowLifecycleTools.class, () -> fixtures.projectWorkflowLifecycleTools)
            .withBean(
                DeploymentToolCallbacksFactory.class,
                () -> new DeploymentToolCallbacksFactory(fixtures.projectDeploymentFacade))
            .withBean(InteractivePickerToolCallbacks.class, () -> fixtures.interactivePickerToolCallbacks)
            .withBean(IntelligentToolCatalog.class, () -> fixtures.intelligentToolCatalog)
            .withBean(WorkspaceFacade.class, () -> fixtures.workspaceFacade)
            .withBean(ProjectWorkflowFacade.class, () -> fixtures.projectWorkflowFacade)
            .withBean(AiHubChatArtifactRecorder.class, () -> fixtures.artifactRecorder)
            .withBean(SecurityContextRehydrator.class, () -> fixtures.securityContextRehydrator)
            .withBean(AiHubChatRepository.class, () -> mock(AiHubChatRepository.class))
            .withBean(AiHubChatArtifactRepository.class, () -> mock(AiHubChatArtifactRepository.class))
            .withBean(AiHubToolApprovalRepository.class, () -> mock(AiHubToolApprovalRepository.class))
            .withBean(AiHubToolApprovalRuleRepository.class, () -> mock(AiHubToolApprovalRuleRepository.class))
            .withBean(JsonMapper.class, () -> JsonMapper.builder()
                .build())
            .withBean(PermissionService.class, () -> mock(PermissionService.class))
            .withBean(ProjectDeploymentFacade.class, () -> mock(ProjectDeploymentFacade.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(UserService.class, () -> mock(UserService.class))
            .withBean("workerExecutor", Executor.class, () -> Runnable::run)
            .withBean(CacheManager.class, ConcurrentMapCacheManager::new)
            .withBean(WebhookWorkflowExecutor.class, () -> mock(WebhookWorkflowExecutor.class))
            .withBean(LocalAgentsHolder.class);
    }

    static final class LocalAgentsHolder {

        private final List<LocalAgent> localAgents;

        LocalAgentsHolder(List<LocalAgent> localAgents) {
            this.localAgents = localAgents;
        }
    }
}
