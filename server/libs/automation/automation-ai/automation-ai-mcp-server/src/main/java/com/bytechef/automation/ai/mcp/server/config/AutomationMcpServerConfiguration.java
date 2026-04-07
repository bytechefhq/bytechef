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

package com.bytechef.automation.ai.mcp.server.config;

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
import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.security.web.configurer.AutomationMcpServerSecurityConfigurer;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
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
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
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
 * @author Ivica Cardic
 */
@Configuration
public class AutomationMcpServerConfiguration {

    public static final String SECRET_KEY = "secretKey";

    @Bean
    WebMvcStreamableServerTransportProvider automationWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/automation/{secretKey}/mcp")
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);

                return McpTransportContext.create(Map.of(SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> automationMcpRouterFunction() {
        return automationWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    AutomationMcpToolFacade mcpToolFacade(
        ChildJobPrincipalFactory childJobPrincipalFactory,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, ContextService contextService,
        CounterService counterService, Environment environment, Evaluator evaluator, JobService jobService,
        McpComponentService mcpComponentService, McpProjectWorkflowService mcpProjectWorkflowService,
        McpServerService mcpServerService, PrincipalJobFacade principalJobFacade,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, SubflowResolver subflowResolver,
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

        return new AutomationMcpToolFacade(
            clusterElementDefinitionFacade, clusterElementDefinitionService, evaluator, jobSyncExecutor,
            mcpComponentService, mcpProjectWorkflowService, mcpServerService, principalJobFacade,
            projectDeploymentWorkflowService, taskExecutionService, taskFileStorage, workflowService);
    }

    @Bean
    FilterableMcpAsyncServer automationMcpAsyncServer(
        McpComponentService mcpComponentService, McpProjectService mcpProjectService,
        McpServerService mcpServerService, McpToolService mcpToolService, AutomationMcpToolFacade mcpToolFacade) {

        return new FilterableMcpServerBuilder(automationWebMvcStreamableHttpServerTransportProvider())
            .serverInfo("automation-mcp-server", "1.0.0")
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

                McpServer mcpServer = mcpServerService.getMcpServer(secretKey);

                List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

                mcpComponentService.getMcpServerMcpComponents(mcpServer.getId())
                    .stream()
                    .flatMap(
                        mcpComponent -> CollectionUtils.stream(
                            mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())))
                    .map(mcpTool -> McpToolUtils.toAsyncToolSpecification(
                        mcpToolFacade.getFunctionToolCallback(mcpTool)))
                    .forEach(tools::add);

                mcpProjectService.getMcpServerMcpProjects(mcpServer.getId())
                    .stream()
                    .flatMap(mcpProject -> CollectionUtils.stream(mcpToolFacade.getFunctionToolCallbacks(mcpProject)))
                    .map(McpToolUtils::toAsyncToolSpecification)
                    .forEach(tools::add);

                return tools;
            })
            .build();
    }

    @Bean
    SecurityConfigurerContributor automationMcpServerSecurityConfigurerContributor(McpServerService mcpServerService) {
        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new AutomationMcpServerSecurityConfigurer(mcpServerService);
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
                taskFileStorage),
            (taskDispatcher) -> new SubflowTaskDispatcher(childJobPrincipalFactory, jobService, subflowResolver),
            (taskDispatcher) -> new WaitForApprovalTaskDispatcher(eventPublisher, jobService, taskExecutionService));
    }
}
