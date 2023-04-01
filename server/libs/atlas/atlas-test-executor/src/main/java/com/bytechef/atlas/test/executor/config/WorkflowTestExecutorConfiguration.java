package com.bytechef.hermes.workflow.web.rest.config;

import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.sync.executor.WorkflowSyncExecutor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowSyncExecutorConfiguration {

    @Bean
    WorkflowSyncExecutor workflowSyncExecutor(
        ObjectMapper objectMapper, List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories,
        Map<String, TaskHandler<?>> taskHandlerMap, List<WorkflowRepository> workflowRepositories) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

        CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(
                new InMemoryJobRepository(taskExecutionRepository, objectMapper), workflowRepositories);

        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        WorkflowService workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories)

        WorkflowSyncExecutor workflowSyncExecutor = new WorkflowSyncExecutor(
            contextService, jobService, e -> {}, taskCompletionHandlerFactories, taskDispatcherResolverFactories,
            taskExecutionService, taskHandlerMap, workflowService);

        return workflowSyncExecutor;
    }
}
