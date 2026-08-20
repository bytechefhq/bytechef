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
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.SampleOutputSpringAIAgent;
import com.bytechef.ai.copilot.agent.SkillsSpringAIAgent;
import com.bytechef.ai.copilot.agent.WorkflowCodeEditorSpringAIAgent;
import com.bytechef.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ai.copilot.agent.WorkflowExecutionSpringAIAgent;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.CreateConnectionToolCallback;
import com.bytechef.ai.copilot.tool.JsonSchemaTools;
import com.bytechef.ai.copilot.tool.ListConnectionsForComponentToolCallback;
import com.bytechef.ai.copilot.tool.LookupActionPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.LookupTriggerPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SampleOutputTools;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.SelectConnectionToolCallback;
import com.bytechef.ai.copilot.tool.SelectPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SelectTriggerPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadSkillsTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.ai.tool.BraveWebSearchTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import com.bytechef.platform.ai.tool.SimulationTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotConfiguration {

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
    private final Resource promptWorkflowExecutionAskResource;
    private final Resource promptWorkflowExecutionBuildResource;
    private final State state = new State();

    // Read once at configuration init rather than per delegation: the corresponding subagent ChatClient factory
    // beans below rebuild their ChatClient on every delegation (to honour a caller-picked ChatModel), and
    // getSystemPrompt(Resource) performs I/O.
    private final String workflowEditorAskSystemPrompt;
    private final String workflowEditorBuildSystemPrompt;
    private final String codeEditorAskSystemPrompt;
    private final String codeEditorBuildSystemPrompt;
    private final String converterBuildSystemPrompt;
    private final String clusterElementAskSystemPrompt;
    private final String clusterElementBuildSystemPrompt;
    private final String skillsAskSystemPrompt;
    private final String skillsBuildSystemPrompt;
    private final String workflowExecutionAskSystemPrompt;
    private final String workflowExecutionBuildSystemPrompt;

    private final ConnectionDefinitionService connectionDefinitionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final PropertyOptionsResolver propertyOptionsResolver;
    private final ObjectProvider<CopilotConnectionLister> connectionListerProvider;
    private final JsonMapper jsonMapper = new JsonMapper();

    // CT_CONSTRUCTOR_THROW: the constructor reads and validates the prompt resources up front (see the hoisted
    // *SystemPrompt fields above) so getSystemPrompt's IllegalStateException surfaces at startup, not on the first
    // delegation; Spring never subclasses this @Configuration in a way that could observe partially-initialized state.
    // RUBY-DISABLED: the prompt resources loaded below had every Ruby reference DELETED, not commented out.
    // readPrompt() below is a verbatim readAllBytes with no comment stripping, so the whole file becomes the
    // system message — a commented-out Ruby section would still be read by the model as content and it would
    // keep offering a language that org.graalvm.polyglot:ruby (stuck at 25.0.0, crashes on the pinned Truffle
    // 25.2.4) can no longer run. The marker therefore lives here, in code the model never sees.
    // Affected: prompt_code_editor_build.txt — the '- Ruby: `def perform(input, context) ... end`' perform-signature
    // bullet. ScriptComponentHandler no longer registers the script/v1/ruby action either, so Ruby written into a
    // Script task could not run. Once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is
    // downgraded), restore that bullet alongside the Script component's Ruby action. Grep RUBY-DISABLED.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public CopilotConfiguration(
        @Value("classpath:prompt_workflow_editor_ask.txt") Resource promptWorkflowEditorAskResource,
        @Value("classpath:prompt_workflow_editor_build.txt") Resource promptWorkflowEditorBuildResource,
        @Value("classpath:prompt_code_editor_ask.txt") Resource promptCodeEditorAskResource,
        @Value("classpath:prompt_code_editor_build.txt") Resource promptCodeEditorBuildResource,
        @Value("classpath:prompt_workflow_code_editor_ask.txt") Resource promptWorkflowCodeEditorAskResource,
        @Value("classpath:prompt_workflow_code_editor_build.txt") Resource promptWorkflowCodeEditorBuildResource,
        @Value("classpath:prompt_converter_build.txt") Resource promptConverterBuildResource,
        @Value("classpath:prompt_cluster_element_ask.txt") Resource promptClusterElementAskResource,
        @Value("classpath:prompt_cluster_element_build.txt") Resource promptClusterElementBuildResource,
        @Value("classpath:prompt_skills_ask.txt") Resource promptSkillsAskResource,
        @Value("classpath:prompt_skills_build.txt") Resource promptSkillsBuildResource,
        @Value("classpath:prompt_json_schema_builder_ask.txt") Resource promptJsonSchemaBuilderAskResource,
        @Value("classpath:prompt_json_schema_builder_build.txt") Resource promptJsonSchemaBuilderBuildResource,
        @Value("classpath:prompt_sample_output_ask.txt") Resource promptSampleOutputAskResource,
        @Value("classpath:prompt_sample_output_build.txt") Resource promptSampleOutputBuildResource,
        WorkflowValidatorTools workflowValidatorTools, WorkflowInstructionTools workflowInstructionTools,
        @Value("classpath:prompt_workflow_execution_ask.txt") Resource promptWorkflowExecutionAskResource,
        @Value("classpath:prompt_workflow_execution_build.txt") Resource promptWorkflowExecutionBuildResource,
        ConnectionDefinitionService connectionDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade,
        ComponentDefinitionService componentDefinitionService, ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, PropertyOptionsResolver propertyOptionsResolver,
        ObjectProvider<CopilotConnectionLister> connectionListerProvider) {

        this.connectionListerProvider = connectionListerProvider;
        this.connectionDefinitionService = connectionDefinitionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
        this.componentDefinitionService = componentDefinitionService;
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
        this.promptWorkflowExecutionAskResource = promptWorkflowExecutionAskResource;
        this.promptWorkflowExecutionBuildResource = promptWorkflowExecutionBuildResource;

        this.workflowEditorAskSystemPrompt = getSystemPrompt(promptWorkflowEditorAskResource);
        this.workflowEditorBuildSystemPrompt = getSystemPrompt(promptWorkflowEditorBuildResource);
        this.codeEditorAskSystemPrompt = getSystemPrompt(promptCodeEditorAskResource);
        this.codeEditorBuildSystemPrompt = getSystemPrompt(promptCodeEditorBuildResource);
        this.converterBuildSystemPrompt = getSystemPrompt(promptConverterBuildResource);
        this.clusterElementAskSystemPrompt = getSystemPrompt(promptClusterElementAskResource);
        this.clusterElementBuildSystemPrompt = getSystemPrompt(promptClusterElementBuildResource);
        this.skillsAskSystemPrompt = getSystemPrompt(promptSkillsAskResource);
        this.skillsBuildSystemPrompt = getSystemPrompt(promptSkillsBuildResource);
        this.workflowExecutionAskSystemPrompt = getSystemPrompt(promptWorkflowExecutionAskResource);
        this.workflowExecutionBuildSystemPrompt = getSystemPrompt(promptWorkflowExecutionBuildResource);
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {
        String name = Source.CODE_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.CODE_EDITOR.name() + "_" + Mode.BUILD.name();

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_CODE_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorAskResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_CODE_EDITOR.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorBuildResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.BUILD.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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

    private List<ToolCallback> interactivePickerToolCallbacks() {
        List<CopilotConnectionLister> connectionListers = new ArrayList<>();

        connectionListers.add(new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, propertyOptionsResolver));

        CopilotConnectionLister embeddedConnectionLister = connectionListerProvider.getIfAvailable();

        if (embeddedConnectionLister != null) {
            connectionListers.add(embeddedConnectionLister);
        }

        return List.of(
            new ListConnectionsForComponentToolCallback(
                componentDefinitionService, connectionDefinitionService, ToolStateVisibilityMetrics.NOOP,
                connectionListers),
            new SelectConnectionToolCallback(componentDefinitionService),
            new LookupActionPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new LookupTriggerPropertyOptionsToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new SelectPropertyOptionToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new SelectTriggerPropertyOptionToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP),
            new CreateConnectionToolCallback(componentDefinitionService));
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        Advisor questionAnswerAdvisor, PermissionService permissionService,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        tools.addAll(interactivePickerToolCallbacks());

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .advisor(questionAnswerAdvisor)
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, ScriptTools scriptTools, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, PermissionService permissionService,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                projectWorkflowTools, componentTools, taskTools, scriptTools, workflowValidatorTools,
                workflowInstructionTools));

        tools.addAll(interactivePickerToolCallbacks());

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorBuildResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
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

        String name = Source.CONVERTER.name() + "_" + Mode.BUILD.name();

        return ConverterSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.SKILLS.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return SkillsSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SkillsSpringAIAgent skillsBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.SKILLS.name() + "_" + Mode.BUILD.name();

        return SkillsSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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

        String name = Source.JSON_SCHEMA_BUILDER.name() + "_" + Mode.ASK.name();

        return JsonSchemaBuilderSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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

        String name = Source.JSON_SCHEMA_BUILDER.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(List.of(new JsonSchemaTools()));

        return JsonSchemaBuilderSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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

        String name = Source.SAMPLE_OUTPUT.name() + "_" + Mode.ASK.name();

        return SampleOutputSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSampleOutputAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of()))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowExecutionSpringAIAgent workflowExecutionAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_EXECUTION.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                workflowExecutionTools, readProjectWorkflowTools, componentTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return WorkflowExecutionSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowExecutionAskResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SampleOutputSpringAIAgent sampleOutputBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.SAMPLE_OUTPUT.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(List.of(new SampleOutputTools()));

        return SampleOutputSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSampleOutputBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowExecutionSpringAIAgent workflowExecutionBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ProjectWorkflowTools projectWorkflowTools, ScriptTools scriptTools, TaskTools taskTools,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_EXECUTION.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        return WorkflowExecutionSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowExecutionBuildResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    /**
     * Stateless Code Editor ASK sub-agent {@link ChatClient}, consumed by {@code CodeEditorAgentToolCallback} on the AI
     * Hub ASK agent. Same system prompt and tool catalog as {@link #codeEditorAskSpringAIAgent} (web search tools added
     * when present in the deployment), no {@link ChatMemory}.
     */
    @Bean
    ChatClient codeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools) {

        return buildCodeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, firecrawlTools, braveWebSearchTools);
    }

    @Bean
    IntelligentToolChatClientFactory codeEditorAskSubAgentChatClientFactory(
        @Qualifier("codeEditorAskSubAgentChatClient") ChatClient codeEditorAskSubAgentChatClient,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeEditorAskSubAgentChatClient
            : buildCodeEditorAskSubAgentChatClient(
                candidateChatModel, readProjectWorkflowTools, componentTools, firecrawlTools, braveWebSearchTools);
    }

    private ChatClient buildCodeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(codeEditorAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        builder.defaultTools(tools.toArray());

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
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        Advisor questionAnswerAdvisor) {

        return buildWorkflowEditorAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools,
            braveWebSearchTools, questionAnswerAdvisor);
    }

    @Bean
    IntelligentToolChatClientFactory workflowEditorAskSubAgentChatClientFactory(
        @Qualifier("workflowEditorAskSubAgentChatClient") ChatClient workflowEditorAskSubAgentChatClient,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools, Advisor questionAnswerAdvisor) {

        return candidateChatModel -> candidateChatModel == null
            ? workflowEditorAskSubAgentChatClient
            : buildWorkflowEditorAskSubAgentChatClient(
                candidateChatModel, readProjectTools, readProjectWorkflowTools, componentTools, taskTools,
                firecrawlTools, braveWebSearchTools, questionAnswerAdvisor);
    }

    private ChatClient buildWorkflowEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        Advisor questionAnswerAdvisor) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(workflowEditorAskSystemPrompt)
            .defaultAdvisors(questionAnswerAdvisor);

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        builder.defaultTools(tools.toArray());

        return builder.build();
    }

    @Bean
    ChatClient workflowEditorBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        SimulationTools simulationTools) {

        return buildWorkflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, simulationTools);
    }

    @Bean
    IntelligentToolChatClientFactory workflowEditorBuildSubAgentChatClientFactory(
        @Qualifier("workflowEditorBuildSubAgentChatClient") ChatClient workflowEditorBuildSubAgentChatClient,
        ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        SimulationTools simulationTools) {

        return candidateChatModel -> candidateChatModel == null
            ? workflowEditorBuildSubAgentChatClient
            : buildWorkflowEditorBuildSubAgentChatClient(
                candidateChatModel, projectWorkflowTools, taskTools, scriptTools, simulationTools);
    }

    private ChatClient buildWorkflowEditorBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        SimulationTools simulationTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(workflowEditorBuildSystemPrompt)
            .defaultTools(
                projectWorkflowTools, taskTools, scriptTools, simulationTools, workflowValidatorTools,
                workflowInstructionTools)
            // One-shot subagent (backs the management MCP workflow_editor agent + AI Hub delegation): give it
            // lookupPropertyOptions so it fetches real option values for dynamic-option properties and sets a valid
            // one itself. Deliberately not the interactive select picker, whose result the client must render and
            // resolve in place. askUserQuestion is not registered here either, but for the opposite reason: the
            // specialist-flavoured one is attached to every intelligent delegate by IntelligentToolCatalog, which
            // owns that wiring so no delegate configuration has to remember it.
            .defaultToolCallbacks(
                new LookupComponentPropertyOptionsToolCallback(
                    actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                    propertyOptionsResolver, ToolStateVisibilityMetrics.NOOP))
            .build();
    }

    @Bean
    ChatClient converterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools) {

        return buildConverterBuildSubAgentChatClient(chatModel, projectWorkflowTools, taskTools, scriptTools);
    }

    @Bean
    IntelligentToolChatClientFactory converterBuildSubAgentChatClientFactory(
        @Qualifier("converterBuildSubAgentChatClient") ChatClient converterBuildSubAgentChatClient,
        ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools) {

        return candidateChatModel -> candidateChatModel == null
            ? converterBuildSubAgentChatClient
            : buildConverterBuildSubAgentChatClient(candidateChatModel, projectWorkflowTools, taskTools, scriptTools);
    }

    private ChatClient buildConverterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(converterBuildSystemPrompt)
            .defaultTools(
                projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools, workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient clusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools) {

        return buildClusterElementAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools, braveWebSearchTools);
    }

    @Bean
    IntelligentToolChatClientFactory clusterElementAskSubAgentChatClientFactory(
        @Qualifier("clusterElementAskSubAgentChatClient") ChatClient clusterElementAskSubAgentChatClient,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools) {

        return candidateChatModel -> candidateChatModel == null
            ? clusterElementAskSubAgentChatClient
            : buildClusterElementAskSubAgentChatClient(
                candidateChatModel, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools,
                braveWebSearchTools);
    }

    private ChatClient buildClusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(clusterElementAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        builder.defaultTools(tools.toArray());

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
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools) {

        return buildSkillsAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools, firecrawlTools,
            braveWebSearchTools);
    }

    @Bean
    IntelligentToolChatClientFactory skillsAskSubAgentChatClientFactory(
        @Qualifier("skillsAskSubAgentChatClient") ChatClient skillsAskSubAgentChatClient,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ReadSkillsTools readSkillsTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools) {

        return candidateChatModel -> candidateChatModel == null
            ? skillsAskSubAgentChatClient
            : buildSkillsAskSubAgentChatClient(
                candidateChatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools, firecrawlTools,
                braveWebSearchTools);
    }

    private ChatClient buildSkillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(skillsAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        builder.defaultTools(tools.toArray());

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

    /**
     * Stateless Workflow Execution ASK sub-agent {@link ChatClient}, consumed by
     * {@code WorkflowExecutionAgentToolCallback} on the AI Hub ASK agent. Same system prompt and tool catalog as
     * {@link #workflowExecutionAskSpringAIAgent} (web search tools added when present in the deployment), no
     * {@link ChatMemory}.
     */
    @Bean
    ChatClient workflowExecutionAskSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools) {

        return buildWorkflowExecutionAskSubAgentChatClient(
            chatModel, workflowExecutionTools, readProjectWorkflowTools, componentTools, firecrawlTools,
            braveWebSearchTools);
    }

    @Bean
    IntelligentToolChatClientFactory workflowExecutionAskSubAgentChatClientFactory(
        @Qualifier("workflowExecutionAskSubAgentChatClient") ChatClient workflowExecutionAskSubAgentChatClient,
        WorkflowExecutionTools workflowExecutionTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools) {

        return candidateChatModel -> candidateChatModel == null
            ? workflowExecutionAskSubAgentChatClient
            : buildWorkflowExecutionAskSubAgentChatClient(
                candidateChatModel, workflowExecutionTools, readProjectWorkflowTools, componentTools, firecrawlTools,
                braveWebSearchTools);
    }

    private ChatClient buildWorkflowExecutionAskSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(workflowExecutionAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                workflowExecutionTools, readProjectWorkflowTools, componentTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        builder.defaultTools(tools.toArray());

        return builder.build();
    }

    /**
     * Stateless Workflow Execution BUILD sub-agent {@link ChatClient}, consumed by
     * {@code WorkflowExecutionAgentToolCallback} on the AI Hub BUILD agent. Mirrors
     * {@link #workflowExecutionBuildSpringAIAgent}'s tool catalog (write-capable project-workflow / script / task
     * tools), no {@link ChatMemory}.
     */
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
