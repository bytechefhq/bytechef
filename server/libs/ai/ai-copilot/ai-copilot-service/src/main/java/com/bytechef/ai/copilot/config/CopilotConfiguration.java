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

package com.bytechef.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.advisor.EnvironmentAwareQuestionAnswerAdvisor;
import com.bytechef.ai.copilot.agent.ClusterElementSpringAIAgent;
import com.bytechef.ai.copilot.agent.CodeEditorSpringAIAgent;
import com.bytechef.ai.copilot.agent.ConverterSpringAIAgent;
import com.bytechef.ai.copilot.agent.JsonSchemaBuilderSpringAIAgent;
import com.bytechef.ai.copilot.agent.McpServerSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.SampleOutputSpringAIAgent;
import com.bytechef.ai.copilot.agent.SkillsSpringAIAgent;
import com.bytechef.ai.copilot.agent.WorkflowCodeEditorSpringAIAgent;
import com.bytechef.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.ConfigureMcpServerToolCallback;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.JsonSchemaTools;
import com.bytechef.ai.copilot.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SampleOutputTools;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ai.copilot.tool.advisor.TokenUsageLoggerAdvisor;
import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.DataTableTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadSkillsTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotConfiguration {

    static final Set<String> CLUSTER_ELEMENT_LOOKUP_TOOL_NAMES = Set.of(
        "getAction", "getClusterElement", "searchClusterElements");

    private static final String BUILD_WORKFLOW_SUBAGENT_ASK_SECTION = """

        ## Asking the user

        When a choice is genuinely the user's and you hold 2 to 4 real options for it (values lookupPropertyOptions
        returned in this delegation, or fixed options from the property definition),
        ask ONE question with askUserQuestion and stop. Every option must be one of those values.
        """;

    private static final String BUILD_WORKFLOW_SUBAGENT_NO_ASK_SECTION = """

        ## Asking the user

        You do not have these tools: askUserQuestion.
        Never ask the user a question. Leave a choice that is genuinely the user's empty and name it in your report.
        """;

    private static final String BUILD_WORKFLOW_SUBAGENT_ADDENDUM = """

        ## Brand-new workflows (overrides step 7's "you always author into an EXISTING workflow" note)

        You are a one-shot subagent. Your caller gives you EITHER a `workflowId` (the workflow already exists) OR a
        `projectId` (the workflow does not exist yet). Build the COMPLETE definition first, exactly as step 7
        requires, then persist it with ONE call:

        - Given a `workflowId` — `getWorkflow(workflowId)` for its current `version`, then
          `updateWorkflow(workflowId, workflow, version)`.
        - Given only a `projectId` — `createProjectWorkflow(projectId, workflow)` carrying the COMPLETE definition.
          Never create an empty or default "Manual" placeholder workflow and fill it in afterwards; the single
          create call must carry the whole workflow.

        Both calls validate the definition first. If they report errors, nothing was saved: fix them and make the
        same call again.

        If you are given both, the `workflowId` wins — update it. Never create a second workflow for a project you
        were already pointed at.

        You still cannot create a PROJECT (`createProject` is not among your tools). Given neither a `workflowId`
        nor a `projectId`, say so plainly and tell the caller to create or choose the project first, then re-invoke
        you with its `projectId`.

        A successful save stored exactly the definition you sent; do not re-read it with `getWorkflow`. Continue
        with step 8 as written. The
        `workflowId | projectId | projectWorkflowId` line is REQUIRED on both branches — `createProjectWorkflow`
        returns all three ids.
        """;

    private static final String CONVERTER_SUBAGENT_ADDENDUM = """

        ## Target workflow

        You are a one-shot subagent. Your caller gives you EITHER a `workflowId` (the workflow already exists) OR a
        `projectId` (the workflow does not exist yet).

        Build and validate the COMPLETE converted definition first, then persist it with ONE call:

        - Given a `workflowId` (a leading `Target workflowId:` line counts) - `getWorkflow(workflowId)` for its
          current `version`, then `updateWorkflow(workflowId, workflow, version)`.
        - Given only a `projectId` - ONE `createProjectWorkflow(projectId, workflow)` call carrying the whole
          definition. Never create an empty or placeholder workflow and fill it in afterwards.

        If you are given both, the `workflowId` wins; never create a second workflow. You cannot create a PROJECT.
        Given neither a `workflowId` nor a `projectId`, say so plainly and tell the caller to create or choose the
        project first, then re-invoke you with its `projectId`.
        """;

    private final Resource promptWorkflowEditorAskResource;
    private final Resource promptWorkflowEditorBuildResource;
    private final Resource promptCodeEditorAskResource;
    private final Resource promptCodeEditorBuildResource;
    private final Resource promptWorkflowCodeEditorAskResource;
    private final Resource promptWorkflowCodeEditorBuildResource;
    private final Resource promptConverterBuildResource;
    private final Resource promptClusterElementAskResource;
    private final Resource promptClusterElementBuildResource;
    private final Resource promptSkillsAskResource;
    private final Resource promptSkillsBuildResource;
    private final Resource promptJsonSchemaBuilderAskResource;
    private final Resource promptJsonSchemaBuilderBuildResource;
    private final Resource promptSampleOutputAskResource;
    private final Resource promptSampleOutputBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();

    private final String workflowEditorAskSystemPrompt;
    private final String workflowEditorBuildSystemPrompt;
    private final String workflowEditorBuildSubAgentSystemPrompt;
    private final String codeEditorAskSystemPrompt;
    private final String codeEditorBuildSystemPrompt;
    private final String converterBuildSystemPrompt;
    private final String clusterElementAskSystemPrompt;
    private final String clusterElementBuildSystemPrompt;
    private final String skillsAskSystemPrompt;
    private final String skillsBuildSystemPrompt;
    private final String workflowExecutionBuildSystemPrompt;

    private final InteractivePickerToolCallbacks interactivePickerToolCallbacks;
    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final PropertyOptionsResolver propertyOptionsResolver;
    private final ScopedConnectionGuard scopedConnectionGuard;
    private final @Nullable DataTableTools dataTableTools;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public CopilotConfiguration(
        @Value("classpath:prompt_workflow_editor_ask.txt") Resource promptWorkflowEditorAskResource,
        @Value("classpath:prompt_workflow_editor_build.txt") Resource promptWorkflowEditorBuildResource,
        @Value("classpath:prompt_workflow_editor_build_sub_agent.txt") Resource promptWorkflowEditorBuildSubAgentResource,
        @Value("classpath:prompt_code_editor_ask.txt") Resource promptCodeEditorAskResource,
        @Value("classpath:prompt_code_editor_build.txt") Resource promptCodeEditorBuildResource,
        @Value("classpath:prompt_workflow_code_editor_ask.txt") Resource promptWorkflowCodeEditorAskResource,
        @Value("classpath:prompt_workflow_code_editor_build.txt") Resource promptWorkflowCodeEditorBuildResource,
        @Value("classpath:prompt_converter_build.txt") Resource promptConverterBuildResource,
        @Value("classpath:prompt_cluster_element_ask.txt") Resource promptClusterElementAskResource,
        @Value("classpath:prompt_cluster_element_build.txt") Resource promptClusterElementBuildResource,
        @Value("classpath:prompt_skills_ask.txt") Resource promptSkillsAskResource,
        @Value("classpath:prompt_skills_build.txt") Resource promptSkillsBuildResource,
        @Value("classpath:prompt_workflow_execution_build.txt") Resource promptWorkflowExecutionBuildResource,
        @Value("classpath:prompt_json_schema_builder_ask.txt") Resource promptJsonSchemaBuilderAskResource,
        @Value("classpath:prompt_json_schema_builder_build.txt") Resource promptJsonSchemaBuilderBuildResource,
        @Value("classpath:prompt_sample_output_ask.txt") Resource promptSampleOutputAskResource,
        @Value("classpath:prompt_sample_output_build.txt") Resource promptSampleOutputBuildResource,
        WorkflowValidatorTools workflowValidatorTools, WorkflowInstructionTools workflowInstructionTools,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, PropertyOptionsResolver propertyOptionsResolver,
        ScopedConnectionGuard scopedConnectionGuard, ObjectProvider<DataTableTools> dataTableToolsProvider) {

        this.dataTableTools = dataTableToolsProvider.getIfAvailable();
        this.interactivePickerToolCallbacks = interactivePickerToolCallbacks;
        this.scopedConnectionGuard = scopedConnectionGuard;
        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.propertyOptionsResolver = propertyOptionsResolver;
        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptWorkflowEditorAskResource = promptWorkflowEditorAskResource;
        this.promptWorkflowEditorBuildResource = promptWorkflowEditorBuildResource;
        this.promptCodeEditorAskResource = promptCodeEditorAskResource;
        this.promptCodeEditorBuildResource = promptCodeEditorBuildResource;
        this.promptWorkflowCodeEditorAskResource = promptWorkflowCodeEditorAskResource;
        this.promptWorkflowCodeEditorBuildResource = promptWorkflowCodeEditorBuildResource;
        this.promptConverterBuildResource = promptConverterBuildResource;
        this.promptClusterElementAskResource = promptClusterElementAskResource;
        this.promptClusterElementBuildResource = promptClusterElementBuildResource;
        this.promptSkillsAskResource = promptSkillsAskResource;
        this.promptSkillsBuildResource = promptSkillsBuildResource;
        this.promptJsonSchemaBuilderAskResource = promptJsonSchemaBuilderAskResource;
        this.promptJsonSchemaBuilderBuildResource = promptJsonSchemaBuilderBuildResource;
        this.promptSampleOutputAskResource = promptSampleOutputAskResource;
        this.promptSampleOutputBuildResource = promptSampleOutputBuildResource;

        this.workflowEditorAskSystemPrompt = getSystemPrompt(promptWorkflowEditorAskResource);
        this.workflowEditorBuildSystemPrompt = getSystemPrompt(promptWorkflowEditorBuildResource);
        this.workflowEditorBuildSubAgentSystemPrompt = getSystemPrompt(promptWorkflowEditorBuildSubAgentResource);
        this.codeEditorAskSystemPrompt = getSystemPrompt(promptCodeEditorAskResource);
        this.codeEditorBuildSystemPrompt = getSystemPrompt(promptCodeEditorBuildResource);
        this.converterBuildSystemPrompt = getSystemPrompt(promptConverterBuildResource);
        this.clusterElementAskSystemPrompt = getSystemPrompt(promptClusterElementAskResource);
        this.clusterElementBuildSystemPrompt = getSystemPrompt(promptClusterElementBuildResource);
        this.skillsAskSystemPrompt = getSystemPrompt(promptSkillsAskResource);
        this.skillsBuildSystemPrompt = getSystemPrompt(promptSkillsBuildResource);
        this.workflowExecutionBuildSystemPrompt = getSystemPrompt(promptWorkflowExecutionBuildResource);
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {
        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        CodeEditorSpringAIAgent springAIAgent = CodeEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.CODE_EDITOR_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return CodeEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.CODE_EDITOR_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorBuildResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        WorkflowCodeEditorSpringAIAgent springAIAgent = WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.WORKFLOW_CODE_EDITOR_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        WorkflowCodeEditorSpringAIAgent springAIAgent = WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.WORKFLOW_CODE_EDITOR_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        ClusterElementSpringAIAgent springAIAgent = ClusterElementSpringAIAgent.builder()
            .agentId(CopilotAgentType.CLUSTER_ELEMENT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return ClusterElementSpringAIAgent.builder()
            .agentId(CopilotAgentType.CLUSTER_ELEMENT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementBuildResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, clusterElementTools, componentTools, taskTools,
                        workflowValidatorTools, workflowInstructionTools)))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    private static List<ToolCallback> getCurrentWebSearchToolCallbacks(
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        if (environment == null) {
            return List.of();
        }

        return webSearchToolCallbackProvider.getToolCallbacks(environment.ordinal());
    }

    private List<ToolCallback> wrapTools(SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : tools) {
            if (tool instanceof ToolCallback toolCallback) {
                toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
            } else {
                for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                    toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
                }
            }
        }

        return toolCallbacks;
    }

    @Bean
    Advisor questionAnswerAdvisor(
        VectorStore copilotPgVectorStore, EmbeddingProviderStatusProvider embeddingProviderStatusProvider) {

        return new EnvironmentAwareQuestionAnswerAdvisor(
            copilotPgVectorStore, embeddingProviderStatusProvider, CopilotConstants.STATE_ENVIRONMENT_ID);
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        WorkflowExecutionTools workflowExecutionTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        Advisor questionAnswerAdvisor, PermissionService permissionService,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowExecutionTools,
                workflowValidatorTools, workflowInstructionTools));

        tools.addAll(interactivePickerToolCallbacks.write());

        WorkflowEditorSpringAIAgent springAIAgent = WorkflowEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.WORKFLOW_EDITOR_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .advisor(questionAnswerAdvisor)
            .advisor(new TokenUsageLoggerAdvisor(CopilotAgentType.WORKFLOW_EDITOR_ASK.key()))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, ScriptTools scriptTools,
        WorkflowExecutionTools workflowExecutionTools, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, PermissionService permissionService,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                projectWorkflowTools, componentTools, taskTools, scriptTools, workflowExecutionTools,
                workflowValidatorTools, workflowInstructionTools));

        if (dataTableTools != null) {
            tools.add(dataTableTools);
        }

        tools.addAll(interactivePickerToolCallbacks.write());

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.WORKFLOW_EDITOR_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorBuildResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .advisor(new TokenUsageLoggerAdvisor(CopilotAgentType.WORKFLOW_EDITOR_BUILD.key()))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ConverterSpringAIAgent converterBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return ConverterSpringAIAgent.builder()
            .agentId(CopilotAgentType.CONVERTER_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptConverterBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SkillsSpringAIAgent skillsAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools));

        SkillsSpringAIAgent springAIAgent = SkillsSpringAIAgent.builder()
            .agentId(CopilotAgentType.SKILLS_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    SkillsSpringAIAgent skillsBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return SkillsSpringAIAgent.builder()
            .agentId(CopilotAgentType.SKILLS_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        skillsTools, readProjectTools, readProjectWorkflowTools, componentTools,
                        workflowValidatorTools, workflowInstructionTools)))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    JsonSchemaBuilderSpringAIAgent jsonSchemaBuilderAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        return JsonSchemaBuilderSpringAIAgent.builder()
            .agentId(CopilotAgentType.JSON_SCHEMA_BUILDER_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptJsonSchemaBuilderAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of()))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    JsonSchemaBuilderSpringAIAgent jsonSchemaBuilderBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(List.of(new JsonSchemaTools()));

        return JsonSchemaBuilderSpringAIAgent.builder()
            .agentId(CopilotAgentType.JSON_SCHEMA_BUILDER_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptJsonSchemaBuilderBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SampleOutputSpringAIAgent sampleOutputAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        return SampleOutputSpringAIAgent.builder()
            .agentId(CopilotAgentType.SAMPLE_OUTPUT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSampleOutputAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of()))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SampleOutputSpringAIAgent sampleOutputBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(List.of(new SampleOutputTools()));

        return SampleOutputSpringAIAgent.builder()
            .agentId(CopilotAgentType.SAMPLE_OUTPUT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSampleOutputBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    McpServerSpringAIAgent mcpServerBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        @Qualifier("mcpServerBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> mcpServerBuildSubAgentChatClientFactoryProvider,
        @Value("classpath:prompt_mcp_server_build.txt") Resource promptMcpServerBuildResource,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider)
        throws AGUIException {

        IntelligentToolChatClientFactory mcpServerChatClientFactory =
            candidateChatModel -> mcpServerBuildSubAgentChatClientFactoryProvider.getObject()
                .get(candidateChatModel);

        return McpServerSpringAIAgent.builder()
            .agentId(CopilotAgentType.MCP_SERVER_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptMcpServerBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        new ConfigureMcpServerToolCallback(
                            mcpServerChatClientFactory, chatModelResolverProvider.getIfAvailable()))))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ChatClient codeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools) {

        return buildCodeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, List.of());
    }

    @Bean
    IntelligentToolChatClientFactory codeEditorAskSubAgentChatClientFactory(
        @Qualifier("codeEditorAskSubAgentChatClient") ChatClient codeEditorAskSubAgentChatClient, ChatModel chatModel,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return codeEditorAskSubAgentChatClient;
            }

            return buildCodeEditorAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectWorkflowTools, componentTools,
                webSearchToolCallbacks);
        };
    }

    private ChatClient buildCodeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, List<ToolCallback> webSearchToolCallbacks) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(codeEditorAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    ChatClient codeEditorBuildSubAgentChatClient(
        ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return buildCodeEditorBuildSubAgentChatClient(chatModel, scriptTools, readProjectWorkflowTools, componentTools);
    }

    @Bean
    IntelligentToolChatClientFactory codeEditorBuildSubAgentChatClientFactory(
        @Qualifier("codeEditorBuildSubAgentChatClient") ChatClient codeEditorBuildSubAgentChatClient,
        ScriptTools scriptTools, ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeEditorBuildSubAgentChatClient
            : buildCodeEditorBuildSubAgentChatClient(
                candidateChatModel, scriptTools, readProjectWorkflowTools, componentTools);
    }

    private ChatClient buildCodeEditorBuildSubAgentChatClient(
        ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(codeEditorBuildSystemPrompt)
            .defaultTools(
                readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient workflowEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Advisor questionAnswerAdvisor) {

        return buildWorkflowEditorAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, componentTools, taskTools, List.of(),
            questionAnswerAdvisor);
    }

    @Bean
    IntelligentToolChatClientFactory workflowEditorAskSubAgentChatClientFactory(
        @Qualifier("workflowEditorAskSubAgentChatClient") ChatClient workflowEditorAskSubAgentChatClient,
        ChatModel chatModel,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        Advisor questionAnswerAdvisor) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return workflowEditorAskSubAgentChatClient;
            }

            return buildWorkflowEditorAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectTools, readProjectWorkflowTools,
                componentTools, taskTools, webSearchToolCallbacks, questionAnswerAdvisor);
        };
    }

    private ChatClient buildWorkflowEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        List<ToolCallback> webSearchToolCallbacks,
        Advisor questionAnswerAdvisor) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(workflowEditorAskSystemPrompt)
            .defaultAdvisors(questionAnswerAdvisor);

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    ChatClient workflowEditorBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        ComponentTools componentTools, ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider) {

        return buildWorkflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, componentTools,
            hasAskTool(subAgentAskRelayProvider), List.of());
    }

    @Bean
    IntelligentToolChatClientFactory workflowEditorBuildSubAgentChatClientFactory(
        @Qualifier("workflowEditorBuildSubAgentChatClient") ChatClient workflowEditorBuildSubAgentChatClient,
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        ComponentTools componentTools, ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        boolean askToolAvailable = hasAskTool(subAgentAskRelayProvider);

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return workflowEditorBuildSubAgentChatClient;
            }

            return buildWorkflowEditorBuildSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, projectWorkflowTools, taskTools,
                scriptTools, componentTools, askToolAvailable, webSearchToolCallbacks);
        };
    }

    private ChatClient buildWorkflowEditorBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        ComponentTools componentTools, boolean askToolAvailable, List<ToolCallback> webSearchToolCallbacks) {

        List<ToolCallback> toolCallbacks = new ArrayList<>(clusterElementLookupToolCallbacks(componentTools));

        toolCallbacks.add(
            new LookupComponentPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                propertyOptionsResolver, ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard));
        toolCallbacks.addAll(webSearchToolCallbacks);

        return ChatClient.builder(chatModel)
            .defaultSystem(buildWorkflowSubAgentSystemPrompt(askToolAvailable))
            .defaultAdvisors(new TokenUsageLoggerAdvisor(CopilotAgentType.BUILD_WORKFLOW.key()))
            .defaultTools(
                withDataTableTools(
                    projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools, workflowInstructionTools))
            .defaultToolCallbacks(toolCallbacks)
            .build();
    }

    static List<ToolCallback> clusterElementLookupToolCallbacks(ComponentTools componentTools) {
        return Arrays.stream(ToolCallbacks.from(componentTools))
            .filter(toolCallback -> CLUSTER_ELEMENT_LOOKUP_TOOL_NAMES.contains(toolCallback.getToolDefinition()
                .name()))
            .toList();
    }

    private static boolean hasAskTool(ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider) {
        return subAgentAskRelayProvider.getIfAvailable() != null;
    }

    String buildWorkflowSubAgentSystemPrompt(boolean askToolAvailable) {
        return workflowEditorBuildSystemPrompt + "\n\n" + workflowEditorBuildSubAgentSystemPrompt +
            BUILD_WORKFLOW_SUBAGENT_ADDENDUM + (askToolAvailable
                ? BUILD_WORKFLOW_SUBAGENT_ASK_SECTION : BUILD_WORKFLOW_SUBAGENT_NO_ASK_SECTION);
    }

    @Bean
    ChatClient converterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools) {

        return buildConverterBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, List.of());
    }

    @Bean
    IntelligentToolChatClientFactory converterBuildSubAgentChatClientFactory(
        @Qualifier("converterBuildSubAgentChatClient") ChatClient converterBuildSubAgentChatClient,
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return converterBuildSubAgentChatClient;
            }

            return buildConverterBuildSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, projectWorkflowTools, taskTools,
                scriptTools, webSearchToolCallbacks);
        };
    }

    private ChatClient buildConverterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools, List<ToolCallback> webSearchToolCallbacks) {

        return ChatClient.builder(chatModel)
            .defaultSystem(converterSubAgentSystemPrompt())
            .defaultTools(
                withDataTableTools(
                    projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools, workflowInstructionTools))
            .defaultToolCallbacks(webSearchToolCallbacks)
            .build();
    }

    private Object[] withDataTableTools(Object... tools) {
        List<Object> allTools = new ArrayList<>(List.of(tools));

        if (dataTableTools != null) {
            allTools.add(dataTableTools);
        }

        return allTools.toArray();
    }

    String converterSubAgentSystemPrompt() {
        return converterBuildSystemPrompt + CONVERTER_SUBAGENT_ADDENDUM;
    }

    @Bean
    ChatClient clusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return buildClusterElementAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, taskTools, List.of());
    }

    @Bean
    IntelligentToolChatClientFactory clusterElementAskSubAgentChatClientFactory(
        @Qualifier("clusterElementAskSubAgentChatClient") ChatClient clusterElementAskSubAgentChatClient,
        ChatModel chatModel,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return clusterElementAskSubAgentChatClient;
            }

            return buildClusterElementAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectWorkflowTools, componentTools,
                taskTools, webSearchToolCallbacks);
        };
    }

    private ChatClient buildClusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, List<ToolCallback> webSearchToolCallbacks) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(clusterElementAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    ChatClient clusterElementBuildSubAgentChatClient(
        ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools) {

        return buildClusterElementBuildSubAgentChatClient(
            chatModel, clusterElementTools, readProjectWorkflowTools, componentTools, taskTools);
    }

    @Bean
    IntelligentToolChatClientFactory clusterElementBuildSubAgentChatClientFactory(
        @Qualifier("clusterElementBuildSubAgentChatClient") ChatClient clusterElementBuildSubAgentChatClient,
        ClusterElementTools clusterElementTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return candidateChatModel -> candidateChatModel == null
            ? clusterElementBuildSubAgentChatClient
            : buildClusterElementBuildSubAgentChatClient(
                candidateChatModel, clusterElementTools, readProjectWorkflowTools, componentTools, taskTools);
    }

    private ChatClient buildClusterElementBuildSubAgentChatClient(
        ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(clusterElementBuildSystemPrompt)
            .defaultTools(
                readProjectWorkflowTools, clusterElementTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient skillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools) {

        return buildSkillsAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools, List.of());
    }

    @Bean
    IntelligentToolChatClientFactory skillsAskSubAgentChatClientFactory(
        @Qualifier("skillsAskSubAgentChatClient") ChatClient skillsAskSubAgentChatClient, ChatModel chatModel,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ReadSkillsTools readSkillsTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return skillsAskSubAgentChatClient;
            }

            return buildSkillsAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectTools, readProjectWorkflowTools,
                readSkillsTools, webSearchToolCallbacks);
        };
    }

    private ChatClient buildSkillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        List<ToolCallback> webSearchToolCallbacks) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(skillsAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    ChatClient skillsBuildSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools) {

        return buildSkillsBuildSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, skillsTools, componentTools);
    }

    @Bean
    IntelligentToolChatClientFactory skillsBuildSubAgentChatClientFactory(
        @Qualifier("skillsBuildSubAgentChatClient") ChatClient skillsBuildSubAgentChatClient,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        SkillsTools skillsTools, ComponentTools componentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? skillsBuildSubAgentChatClient
            : buildSkillsBuildSubAgentChatClient(
                candidateChatModel, readProjectTools, readProjectWorkflowTools, skillsTools, componentTools);
    }

    private ChatClient buildSkillsBuildSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(skillsBuildSystemPrompt)
            .defaultTools(
                skillsTools, readProjectTools, readProjectWorkflowTools, componentTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient workflowExecutionBuildSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return buildWorkflowExecutionBuildSubAgentChatClient(
            chatModel, workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools);
    }

    @Bean
    IntelligentToolChatClientFactory workflowExecutionBuildSubAgentChatClientFactory(
        @Qualifier("workflowExecutionBuildSubAgentChatClient") ChatClient workflowExecutionBuildSubAgentChatClient,
        WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return candidateChatModel -> candidateChatModel == null
            ? workflowExecutionBuildSubAgentChatClient
            : buildWorkflowExecutionBuildSubAgentChatClient(
                candidateChatModel, workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools);
    }

    private ChatClient buildWorkflowExecutionBuildSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(workflowExecutionBuildSystemPrompt)
            .defaultTools(
                workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    private String getSystemPrompt(Resource systemPromptResource) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
