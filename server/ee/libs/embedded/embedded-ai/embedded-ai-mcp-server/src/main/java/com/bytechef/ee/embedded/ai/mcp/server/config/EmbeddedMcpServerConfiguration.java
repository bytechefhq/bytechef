/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer.EmbeddedMcpServerSecurityConfigurer;
import com.bytechef.ee.embedded.ai.mcp.server.service.ConnectTokenService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.file.storage.InMemoryTaskFileStorage;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.server.FilterableMcpAsyncServer;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.task.dispatcher.approval.WaitForApprovalTaskDispatcher;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.each.EachTaskDispatcher;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcher;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcher;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.bytechef.tenant.TenantContext;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConditionalOnEEVersion
@Configuration
public class EmbeddedMcpServerConfiguration {

    public static final String EXTERNAL_USER_ID = "externalUserId";
    public static final String SECRET_KEY = "secretKey";

    @Bean
    WebMvcStreamableServerTransportProvider embeddedWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/embedded/{secretKey}/mcp")
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);
                String externalUserId = serverRequest.param(EXTERNAL_USER_ID)
                    .orElse("");

                return McpTransportContext.create(Map.of(SECRET_KEY, secretKey, EXTERNAL_USER_ID, externalUserId));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> embeddedMcpRouterFunction() {
        return embeddedWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    ConnectTokenService connectTokenService() {
        return new ConnectTokenService();
    }

    @Bean
    EmbeddedMcpToolFacade embeddedMcpToolFacade(
        ApplicationProperties applicationProperties,
        ChildJobPrincipalFactory childJobPrincipalFactory,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectedUserService connectedUserService,
        ContextService contextService, CounterService counterService, Environment environment,
        Evaluator evaluator,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, JobService jobService,
        McpComponentService mcpComponentService, McpIntegrationWorkflowService mcpIntegrationWorkflowService,
        McpServerService mcpServerService, PrincipalJobFacade principalJobFacade, SubflowResolver subflowResolver,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        TaskExecutionService taskExecutionService, TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry,
        WorkflowService workflowService) {

        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(environment);

        TaskFileStorage taskFileStorage = new InMemoryTaskFileStorage();

        ApplicationEventPublisher coordinatorEventPublisher = createEventPublisher(asyncMessageBroker);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, asyncMessageBroker,
            getAdditionalApplicationEventListeners(
                evaluator, coordinatorEventPublisher, jobService, taskExecutionService, taskFileStorage),
            getTaskCompletionHandlerFactories(
                contextService, counterService, evaluator, taskExecutionService, taskFileStorage),
            getTaskDispatcherAdapterFactories(evaluator), taskDispatcherPreSendProcessors,
            getTaskDispatcherResolverFactories(
                childJobPrincipalFactory, contextService, counterService, coordinatorEventPublisher, evaluator,
                jobService, subflowResolver, taskExecutionService, taskFileStorage),
            taskExecutionService, taskExecutor, taskHandlerRegistry, taskFileStorage, 300, workflowService);

        return new EmbeddedMcpToolFacade(
            clusterElementDefinitionFacade, clusterElementDefinitionService, connectedUserService,
            connectTokenService(), evaluator, integrationInstanceConfigurationWorkflowService,
            integrationInstanceService, jobSyncExecutor, mcpComponentService, mcpIntegrationWorkflowService,
            mcpServerService, principalJobFacade, applicationProperties.getPublicUrl(), taskExecutionService,
            taskFileStorage, workflowService);
    }

    @Bean
    FilterableMcpAsyncServer embeddedMcpAsyncServer(
        McpComponentService mcpComponentService, McpIntegrationService mcpIntegrationService,
        McpServerService mcpServerService, McpToolService mcpToolService,
        EmbeddedMcpToolFacade embeddedMcpToolFacade) {

        return new FilterableMcpServerBuilder(embeddedWebMvcStreamableHttpServerTransportProvider())
            .serverInfo("embedded-mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .toolFilter((exchange) -> {
                McpTransportContext mcpTransportContext = exchange.transportContext();

                Object secretKeyObject = mcpTransportContext.get(SECRET_KEY);

                if (secretKeyObject == null) {
                    return List.of();
                }

                String secretKey = secretKeyObject.toString();

                Object externalUserIdObject = mcpTransportContext.get(EXTERNAL_USER_ID);

                if (externalUserIdObject == null || externalUserIdObject.toString()
                    .isEmpty()) {
                    return List.of();
                }

                String externalUserId = externalUserIdObject.toString();

                McpServer mcpServer = mcpServerService.getMcpServer(secretKey);

                String tenantId = mcpServer.getSecretKey();

                List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

                mcpComponentService.getMcpServerMcpComponents(mcpServer.getId())
                    .stream()
                    .flatMap(
                        mcpComponent -> CollectionUtils.stream(
                            mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())))
                    .map(mcpTool -> McpToolUtils.toAsyncToolSpecification(
                        embeddedMcpToolFacade.getFunctionToolCallback(mcpTool, externalUserId, tenantId)))
                    .forEach(tools::add);

                mcpIntegrationService.getMcpServerMcpIntegrations(mcpServer.getId())
                    .stream()
                    .flatMap(mcpIntegration -> CollectionUtils.stream(
                        embeddedMcpToolFacade.getFunctionToolCallbacks(mcpIntegration, externalUserId, tenantId)))
                    .map(McpToolUtils::toAsyncToolSpecification)
                    .forEach(tools::add);

                return tools;
            })
            .build();
    }

    @Bean
    SecurityConfigurerContributor embeddedMcpServerSecurityConfigurerContributor(McpServerService mcpServerService) {
        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new EmbeddedMcpServerSecurityConfigurer(mcpServerService);
            }
        };
    }

    private static ApplicationEventPublisher createEventPublisher(MessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(((MessageEvent<?>) event).getRoute(), event);
        };
    }

    private static List<ApplicationEventListener> getAdditionalApplicationEventListeners(
        Evaluator evaluator, ApplicationEventPublisher coordinatorEventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
            new SubflowJobStatusEventListener(
                evaluator, coordinatorEventPublisher, jobService, taskExecutionService, taskFileStorage));
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new EachTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService),
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                contextService, counterService, evaluator, taskExecutionService, taskCompletionHandler, taskDispatcher,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(
                contextService, counterService, evaluator, taskDispatcher, taskCompletionHandler, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new ParallelTaskCompletionHandler(
                counterService, taskCompletionHandler, taskExecutionService));
    }

    private List<TaskDispatcherAdapterFactory> getTaskDispatcherAdapterFactories(Evaluator evaluator) {
        return List.of(
            new TaskDispatcherAdapterFactory() {

                @Override
                public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver) {
                    return new MapTaskDispatcherAdapterTaskHandler(evaluator, taskHandlerResolver);
                }

                @Override
                public String getName() {
                    return MapConstants.MAP + "/v1";
                }
            });
    }

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ChildJobPrincipalFactory childJobPrincipalFactory, ContextService contextService,
        CounterService counterService, ApplicationEventPublisher eventPublisher,
        Evaluator evaluator, JobService jobService, SubflowResolver subflowResolver,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        return List.of(
            (taskDispatcher) -> new SubflowTaskDispatcher(
                childJobPrincipalFactory, jobService, subflowResolver),
            (taskDispatcher) -> new WaitForApprovalTaskDispatcher(
                eventPublisher, jobService, taskExecutionService),
            (taskDispatcher) -> new BranchTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new EachTaskDispatcher(
                contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new MapTaskDispatcher(
                contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new ParallelTaskDispatcher(
                contextService, counterService, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }
}
