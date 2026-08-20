/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.tool.AgentTypeRegistry;
import com.bytechef.ai.copilot.config.ToolCallbackContributorConfiguration;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ee.automation.ai.copilot.config.AutomationCopilotMcpContributorConfiguration;
import com.bytechef.ee.embedded.ai.copilot.config.EmbeddedCopilotMcpContributorConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Surface-parity guard for the intelligent tool catalog (ticket 732). The catalog is fed by four
 * {@link IntelligentToolContributor} beans — CE {@code CopilotIntelligentToolContributorConfiguration}, CE automation
 * {@code McpServerIntelligentToolContributorConfiguration}, EE automation
 * {@code AutomationIntelligentToolContributorConfiguration}, and EE embedded
 * {@code EmbeddedIntelligentToolContributorConfiguration} — each package-private in its own module, reached here by
 * reflection so the test can assemble the real production definitions without those modules exposing their
 * configuration classes.
 *
 * <p>
 * {@code McpServerIntelligentToolContributorConfiguration} lives in CE {@code automation-ai-tool} rather than the EE
 * {@code AutomationIntelligentToolContributorConfiguration} (automation-ai-copilot) despite both being
 * "automation-owned": {@code configureMcpServer}'s {@code ChatClient} closes over CE-only MCP services, and MCP servers
 * are a CE capability available whenever either the Copilot panel or the AI Hub surface is enabled — unlike
 * {@code buildCustomComponent}/{@code buildCodeWorkflow}, which are genuinely EE-gated.
 * </p>
 *
 * <p>
 * Every surface that narrows the catalog with {@link IntelligentToolCatalog#getByNames} declares its owned name set as
 * a {@code public static final Set<String>} constant: the three management-MCP contributor configs
 * ({@link ToolCallbackContributorConfiguration#INTELLIGENT_TOOL_NAMES} — which also claims {@code configureMcpServer}
 * even though that definition is contributed from a different module, since {@code getByNames} filters the whole
 * catalog by name regardless of origin — {@link AutomationCopilotMcpContributorConfiguration#INTELLIGENT_TOOL_NAMES},
 * {@link EmbeddedCopilotMcpContributorConfiguration#INTELLIGENT_TOOL_NAMES}) and the AI Hub
 * ({@link AiHubConfiguration#INTELLIGENT_TOOL_NAMES}). This test is what makes the partition assertable rather than
 * merely narrated in Javadoc.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntelligentToolSurfaceParityTest {

    private static final Set<String> EXPECTED_CATALOG_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript", "authorSkill",
        "debugWorkflowExecution", "buildCustomComponent", "buildCodeWorkflow", "buildIntegrationWorkflow",
        "configureMcpServer");

    private static final String COPILOT_CONTRIBUTOR_CLASS_NAME =
        "com.bytechef.ai.copilot.config.CopilotIntelligentToolContributorConfiguration";
    private static final String COPILOT_CONTRIBUTOR_METHOD_NAME = "copilotIntelligentToolContributor";

    // The @Qualifier bean names copilotIntelligentToolContributor resolves, in declaration order — each one is the
    // method name of the IntelligentToolChatClientFactory @Bean it names in CopilotConfiguration (ticket 732 Task 5:
    // every delegate's factory now honours the caller-picked ChatModel, so the contributor resolves the factory bean
    // directly instead of the plain ChatClient/Supplier<ChatClient> bean). Verified against that class before being
    // pinned here; do not "fix" the contributor to match a wrong guess at this list.
    private static final Set<String> COPILOT_CONTRIBUTOR_EXPECTED_QUALIFIERS = Set.of(
        "workflowEditorAskSubAgentChatClientFactory", "workflowEditorBuildSubAgentChatClientFactory",
        "converterBuildSubAgentChatClientFactory", "clusterElementAskSubAgentChatClientFactory",
        "clusterElementBuildSubAgentChatClientFactory", "codeEditorAskSubAgentChatClientFactory",
        "codeEditorBuildSubAgentChatClientFactory", "skillsAskSubAgentChatClientFactory",
        "skillsBuildSubAgentChatClientFactory", "workflowExecutionAskSubAgentChatClientFactory",
        "workflowExecutionBuildSubAgentChatClientFactory");

    private static final String AUTOMATION_CONTRIBUTOR_CLASS_NAME =
        "com.bytechef.ee.automation.ai.copilot.config.AutomationIntelligentToolContributorConfiguration";
    private static final String AUTOMATION_CONTRIBUTOR_METHOD_NAME = "automationIntelligentToolContributor";

    // Verified against the @Bean methods in AutomationCopilotConfiguration.
    private static final Set<String> AUTOMATION_CONTRIBUTOR_EXPECTED_QUALIFIERS = Set.of(
        "customComponentAskSubAgentChatClientFactory", "customComponentBuildSubAgentChatClientFactory",
        "codeWorkflowAskSubAgentChatClientFactory", "codeWorkflowBuildSubAgentChatClientFactory");

    private static final String EMBEDDED_CONTRIBUTOR_CLASS_NAME =
        "com.bytechef.ee.embedded.ai.copilot.config.EmbeddedIntelligentToolContributorConfiguration";
    private static final String EMBEDDED_CONTRIBUTOR_METHOD_NAME = "embeddedIntelligentToolContributor";

    // Verified against the @Bean method in EmbeddedCopilotConfiguration.
    private static final Set<String> EMBEDDED_CONTRIBUTOR_EXPECTED_QUALIFIERS =
        Set.of("workflowEditorEmbeddedBuildSubAgentChatClientFactory");

    private static final String AUTOMATION_TOOL_CONTRIBUTOR_CLASS_NAME =
        "com.bytechef.automation.ai.tool.McpServerIntelligentToolContributorConfiguration";
    private static final String AUTOMATION_TOOL_CONTRIBUTOR_METHOD_NAME = "mcpServerIntelligentToolContributor";

    // Verified against the @Bean methods in McpServerSubAgentConfiguration (automation-ai-tool).
    private static final Set<String> AUTOMATION_TOOL_CONTRIBUTOR_EXPECTED_QUALIFIERS =
        Set.of("mcpServerBuildSubAgentChatClientFactory");

    @Test
    void testCatalogContributesExactlyTheTenExpectedNames() throws ReflectiveOperationException {
        IntelligentToolCatalog catalog = buildCatalog();

        List<String> catalogNames = catalog.getNames();

        assertThat(new HashSet<>(catalogNames))
            .as("catalog.getNames() vs the ten expected intelligent tool names")
            .isEqualTo(EXPECTED_CATALOG_NAMES);
        assertThat(catalogNames)
            .as("catalog.getNames() must not contain a duplicate registration")
            .hasSize(EXPECTED_CATALOG_NAMES.size());
    }

    @Test
    void testManagementMcpNameSetsArePairwiseDisjoint() {
        assertThat(intersectionOf(
            ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES,
            AutomationCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES))
                .as("CE ToolCallbackContributorConfiguration and EE AutomationCopilotMcpContributorConfiguration"
                    + " both claim: %s")
                .isEmpty();
        assertThat(intersectionOf(
            ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES,
            EmbeddedCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES))
                .as("CE ToolCallbackContributorConfiguration and EE EmbeddedCopilotMcpContributorConfiguration"
                    + " both claim: %s")
                .isEmpty();
        assertThat(intersectionOf(
            AutomationCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES,
            EmbeddedCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES))
                .as("EE AutomationCopilotMcpContributorConfiguration and EE EmbeddedCopilotMcpContributorConfiguration"
                    + " both claim: %s")
                .isEmpty();
    }

    @Test
    void testManagementMcpNameSetsUnionEqualsTheCatalogNames() throws ReflectiveOperationException {
        Set<String> managementMcpUnion = new HashSet<>();

        managementMcpUnion.addAll(ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES);
        managementMcpUnion.addAll(AutomationCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES);
        managementMcpUnion.addAll(EmbeddedCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES);

        IntelligentToolCatalog catalog = buildCatalog();

        assertThat(managementMcpUnion)
            .as("union of the three management-MCP name sets vs catalog.getNames() — every intelligent tool must"
                + " reach the MCP surface exactly once")
            .isEqualTo(new HashSet<>(catalog.getNames()));
    }

    @Test
    void testHubNameSetEqualsTheCatalogNamesMinusIntegrationWorkflowAgent() {
        Set<String> expectedHubNames = new HashSet<>(EXPECTED_CATALOG_NAMES);

        expectedHubNames.remove("buildIntegrationWorkflow");

        // buildIntegrationWorkflow is the ONE deliberate hub/MCP parity gap: it is an embedded-product delegate
        // (the embedded counterpart of buildWorkflow) that reaches only the embedded management-MCP
        // surface today. The AI Hub has never registered it and its prompts do not mention it. Later narrowing
        // work closes this gap; until then this assertion pins it as intentional rather than an oversight.
        assertThat(AiHubConfiguration.INTELLIGENT_TOOL_NAMES)
            .as("AiHubConfiguration.INTELLIGENT_TOOL_NAMES vs (the ten catalog names - buildIntegrationWorkflow)")
            .isEqualTo(expectedHubNames);
    }

    @Test
    void testEveryNameOnEverySurfaceIsAContributedCatalogName() throws ReflectiveOperationException {
        Set<String> catalogNames = new HashSet<>(buildCatalog().getNames());

        assertUnknownNamesAreEmpty(
            "CE ToolCallbackContributorConfiguration", ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES,
            catalogNames);
        assertUnknownNamesAreEmpty(
            "EE AutomationCopilotMcpContributorConfiguration",
            AutomationCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES, catalogNames);
        assertUnknownNamesAreEmpty(
            "EE EmbeddedCopilotMcpContributorConfiguration",
            EmbeddedCopilotMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES, catalogNames);
        assertUnknownNamesAreEmpty("AiHubConfiguration", AiHubConfiguration.INTELLIGENT_TOOL_NAMES, catalogNames);
    }

    @Test
    void testContributorFactoryMethodsAreActualSpringBeanMethods() throws ReflectiveOperationException {
        // Reaching each contributor by reflection (see buildCatalog()) proves the definitions it returns are
        // correct, but says nothing about whether Spring would ever actually call that method: deleting @Bean (or
        // the enclosing @Configuration) while keeping the method body would drop these delegates from production
        // with every other test in this class still green. This is the one assertion that closes that hole.
        assertFactoryMethodIsASpringBeanMethod(COPILOT_CONTRIBUTOR_CLASS_NAME, COPILOT_CONTRIBUTOR_METHOD_NAME);
        assertFactoryMethodIsASpringBeanMethod(AUTOMATION_CONTRIBUTOR_CLASS_NAME, AUTOMATION_CONTRIBUTOR_METHOD_NAME);
        assertFactoryMethodIsASpringBeanMethod(EMBEDDED_CONTRIBUTOR_CLASS_NAME, EMBEDDED_CONTRIBUTOR_METHOD_NAME);
        assertFactoryMethodIsASpringBeanMethod(
            AUTOMATION_TOOL_CONTRIBUTOR_CLASS_NAME, AUTOMATION_TOOL_CONTRIBUTOR_METHOD_NAME);
    }

    @Test
    void testContributorFactoryMethodQualifiersMatchTheDeclaringChatClientBeanNames()
        throws ReflectiveOperationException {

        // A mistyped @Qualifier string resolves to getIfAvailable() == null and the definition is silently skipped
        // — from all four surfaces at once, since the catalog is now the single construction point. The mocked
        // providers in buildCatalog() are always "present" regardless of the qualifier string, so they cannot catch
        // this; only reflecting on the declared qualifier values against the real IntelligentToolChatClientFactory
        // bean names can.
        assertFactoryMethodQualifiers(
            COPILOT_CONTRIBUTOR_CLASS_NAME, COPILOT_CONTRIBUTOR_METHOD_NAME,
            COPILOT_CONTRIBUTOR_EXPECTED_QUALIFIERS);
        assertFactoryMethodQualifiers(
            AUTOMATION_CONTRIBUTOR_CLASS_NAME, AUTOMATION_CONTRIBUTOR_METHOD_NAME,
            AUTOMATION_CONTRIBUTOR_EXPECTED_QUALIFIERS);
        assertFactoryMethodQualifiers(
            EMBEDDED_CONTRIBUTOR_CLASS_NAME, EMBEDDED_CONTRIBUTOR_METHOD_NAME,
            EMBEDDED_CONTRIBUTOR_EXPECTED_QUALIFIERS);
        assertFactoryMethodQualifiers(
            AUTOMATION_TOOL_CONTRIBUTOR_CLASS_NAME, AUTOMATION_TOOL_CONTRIBUTOR_METHOD_NAME,
            AUTOMATION_TOOL_CONTRIBUTOR_EXPECTED_QUALIFIERS);
    }

    @Test
    void testEveryContributedDefinitionAgentTypeKeyIsRegisteredWithAgentTypeRegistry()
        throws ReflectiveOperationException {

        Set<String> registeredAgentTypeKeys = AgentTypeRegistry.keys();

        List<IntelligentToolDefinition> definitions = contributedDefinitions();

        assertThat(definitions).isNotEmpty();

        for (IntelligentToolDefinition definition : definitions) {
            assertThat(registeredAgentTypeKeys)
                .as(
                    "%s#agentTypeKey() ('%s') must be a key registered with AgentTypeRegistry — an unregistered key"
                        + " would leave a per-conversation session memory row that task delete's registry-driven"
                        + " purge could never reconstruct and clean up",
                    definition.name(), definition.agentTypeKey())
                .contains(definition.agentTypeKey());
        }
    }

    @Test
    void testEveryContributedDefinitionChatClientFactoryHonoursTheCallerPickedChatModel()
        throws ReflectiveOperationException {

        // Ticket 732: ten intelligent delegates now resolve a per-invocation ChatModel (the one the caller picked in
        // the AI Hub composer or a Copilot panel toolbar) instead of always using the contributor's default client.
        // Task 5's own test only proved this for the six CE Copilot definitions; this is the one place that sees all
        // ten tools across all four contributors (CE Copilot, EE automation, EE embedded, CE automation-tool), so it
        // is the only test that can catch a definition wired to ignore the override.
        List<IntelligentToolDefinition> definitions = contributedDefinitions();
        ChatModel chatModel = mock(ChatModel.class);
        int testedVariantCount = 0;

        for (IntelligentToolDefinition definition : definitions) {
            for (IntelligentToolVariant variant : IntelligentToolVariant.values()) {
                IntelligentToolChatClientFactory chatClientFactory = definition.chatClientFactory(variant);

                if (chatClientFactory == null) {
                    continue;
                }

                ChatClient defaultChatClient = chatClientFactory.get(null);
                ChatClient chatModelChatClient = chatClientFactory.get(chatModel);

                assertThat(chatModelChatClient)
                    .as(
                        "%s#chatClientFactory(%s) must rebuild its ChatClient over the caller-picked ChatModel —"
                            + " get(chatModel) returned the same client as get(null), meaning this definition ignores"
                            + " the override",
                        definition.name(), variant)
                    .isNotSameAs(defaultChatClient);

                testedVariantCount++;
            }
        }

        assertThat(testedVariantCount)
            .as(
                "total (definition, variant) pairs with a non-null chatClientFactory — one per"
                    + " IntelligentToolChatClientFactory @Qualifier across all four contributors (11 copilot + 4"
                    + " automation + 1 embedded + 1 automation-tool)")
            .isEqualTo(17);
    }

    @Test
    void testEveryDefinitionNameMatchesTheNameItsOwnCallbackAdvertises() throws ReflectiveOperationException {
        // Every intelligent tool's name is written down twice: once as the definition's name() (the catalog's
        // registration key) and once as a hardcoded default baked into the callback's short constructor (e.g.
        // ProjectWorkflowAgentToolCallback delegates to this(..., "buildWorkflow", ...)). Nothing else asserts the
        // two agree — if they ever diverge, the catalog advertises one name while the callback answers to another,
        // and the model calls a tool that never resolves (a runtime "No ToolCallback found", invisible at compile
        // time). This walks the catalog data-driven, over whatever contributedDefinitions() returns, rather than a
        // hardcoded list of names, so a tool added later is covered automatically.
        List<IntelligentToolDefinition> definitions = contributedDefinitions();

        assertThat(definitions).isNotEmpty();

        for (IntelligentToolDefinition definition : definitions) {
            IntelligentToolChatClientFactory chatClientFactory = firstNonNullChatClientFactory(definition);

            assertThat(chatClientFactory)
                .as(
                    "%s#chatClientFactory(variant) returned null for every IntelligentToolVariant — cannot build its"
                        + " ToolCallback to check the name",
                    definition.name())
                .isNotNull();

            ToolCallback toolCallback = definition.create(chatClientFactory);
            ToolDefinition callbackToolDefinition = toolCallback.getToolDefinition();

            assertThat(callbackToolDefinition.name())
                .as(
                    "intelligent tool '%s': the catalog definition's name() must equal the name its own ToolCallback"
                        + " advertises via ToolDefinition#name() — catalog says '%s', callback says '%s'; a mismatch"
                        + " means the model would call a tool name that never resolves to this callback",
                    definition.name(), definition.name(), callbackToolDefinition.name())
                .isEqualTo(definition.name());
        }
    }

    /**
     * The invariant behind {@link IntelligentToolDefinition#askCapable()}: a delegate whose own system prompt forbids
     * asking must not be handed the specialist {@code askUserQuestion} tool, because
     * {@code IntelligentToolCatalog#buildToolCallback} otherwise attaches it to every delegate it builds.
     *
     * <p>
     * The general form — "for every definition, read the prompt behind it and assert the two agree" — is NOT checkable
     * here, and deliberately so: a definition exposes its {@code chatClientFactory}, never the prompt resource that
     * factory closed over, so there is no mapping from a definition name to a {@code prompt_*.txt} to read. Rather than
     * hardcode that mapping (which would itself be a claim that could drift), this asserts the one concrete case plus
     * its premise — that {@code prompt_converter_build.txt} still carries the never-ask directive — so relaxing the
     * prompt and flipping the flag both fail here.
     * </p>
     */
    @Test
    void testConverterDelegateIsNotAskCapableWhileItsPromptForbidsAsking() throws ReflectiveOperationException {
        String converterPrompt = readClasspathResource("prompt_converter_build.txt");

        assertThat(converterPrompt)
            .as(
                "premise of this test: prompt_converter_build.txt forbids asking. If this directive was deliberately"
                    + " removed, revisit importWorkflow's askCapable() opt-out rather than only this assertion")
            .contains("You must NEVER ask the user any questions");

        List<IntelligentToolDefinition> definitions = contributedDefinitions();

        IntelligentToolDefinition converterDefinition = definitions.stream()
            .filter(definition -> "importWorkflow".equals(definition.name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no 'importWorkflow' definition was contributed to the catalog"));

        assertThat(converterDefinition.askCapable())
            .as(
                "importWorkflow is backed by prompt_converter_build.txt, which forbids asking and requires a bare-JSON"
                    + " answer; an attached askUserQuestion tool would contradict its prompt and could return a"
                    + " question envelope where the caller's contract expects a workflow definition")
            .isFalse();

        for (IntelligentToolDefinition definition : definitions) {
            if (definition == converterDefinition) {
                continue;
            }

            assertThat(definition.askCapable())
                .as(
                    "intelligent tool '%s': ask-capability is the default and the converter is the only opt-out today."
                        + " If another delegate's prompt started forbidding asking, add it above rather than relaxing"
                        + " this assertion",
                    definition.name())
                .isTrue();
        }
    }

    private static String readClasspathResource(String name) {
        ClassLoader classLoader = IntelligentToolSurfaceParityTest.class.getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new AssertionError("classpath resource not found: " + name);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("failed to read classpath resource: " + name, exception);
        }
    }

    @Nullable
    private static IntelligentToolChatClientFactory firstNonNullChatClientFactory(
        IntelligentToolDefinition definition) {

        for (IntelligentToolVariant variant : IntelligentToolVariant.values()) {
            IntelligentToolChatClientFactory chatClientFactory = definition.chatClientFactory(variant);

            if (chatClientFactory != null) {
                return chatClientFactory;
            }
        }

        return null;
    }

    private static void assertFactoryMethodIsASpringBeanMethod(String className, String methodName)
        throws ReflectiveOperationException {

        Method factoryMethod = declaredFactoryMethod(className, methodName);
        Class<?> declaringClass = factoryMethod.getDeclaringClass();

        assertThat(factoryMethod.isAnnotationPresent(Bean.class))
            .as(
                "%s#%s must carry @Bean — without it Spring never registers the method's result and this"
                    + " contributor's delegates silently disappear from every surface",
                className, methodName)
            .isTrue();
        assertThat(declaringClass.isAnnotationPresent(Configuration.class))
            .as(
                "%s must carry @Configuration — without it Spring never processes %s#%s as a bean method",
                className, className, methodName)
            .isTrue();
    }

    private static void assertFactoryMethodQualifiers(
        String className, String methodName, Set<String> expectedQualifierNames) throws ReflectiveOperationException {

        Method factoryMethod = declaredFactoryMethod(className, methodName);
        Parameter[] parameters = factoryMethod.getParameters();

        Set<String> actualQualifierNames = new HashSet<>();
        int qualifiableParameterCount = 0;

        for (Parameter parameter : parameters) {
            // The ObjectProvider<SubAgentChatModelResolver> parameter is deliberately unqualified: unlike the
            // per-domain IntelligentToolChatClientFactory beans, there is exactly one SubAgentChatModelResolver bean
            // to resolve by type, so a @Qualifier would name nothing distinguishing.
            if (isSubAgentChatModelResolverProvider(parameter)) {
                continue;
            }

            qualifiableParameterCount++;

            Qualifier qualifier = parameter.getAnnotation(Qualifier.class);

            assertThat(qualifier)
                .as(
                    "%s#%s has a parameter with no @Qualifier — every IntelligentToolChatClientFactory parameter must"
                        + " name the bean it resolves",
                    className, methodName)
                .isNotNull();

            actualQualifierNames.add(qualifier.value());
        }

        assertThat(actualQualifierNames)
            .as("%s#%s @Qualifier values vs the ChatClient bean names actually declared for it", className,
                methodName)
            .isEqualTo(expectedQualifierNames);
        assertThat(actualQualifierNames)
            .as("%s#%s must not declare the same @Qualifier value twice", className, methodName)
            .hasSize(qualifiableParameterCount);
    }

    private static void assertUnknownNamesAreEmpty(
        String surfaceName, Set<String> surfaceNames, Set<String> catalogNames) {

        Set<String> unknownNames = new HashSet<>(surfaceNames);

        unknownNames.removeAll(catalogNames);

        assertThat(unknownNames)
            .as("%s references intelligent tool names the catalog does not contribute: %s", surfaceName,
                unknownNames)
            .isEmpty();
    }

    private static Set<String> intersectionOf(Set<String> firstNames, Set<String> secondNames) {
        Set<String> intersection = new HashSet<>(firstNames);

        intersection.retainAll(secondNames);

        return intersection;
    }

    /**
     * Assembles the catalog over the four real production {@link IntelligentToolContributor} beans, each reached by
     * reflection because its declaring {@code @Configuration} class is package-private in a module this test does not
     * otherwise depend on by source. Every {@code IntelligentToolChatClientFactory} constructor argument is mocked
     * present so every one of the ten definitions is actually contributed.
     */
    private static IntelligentToolCatalog buildCatalog() throws ReflectiveOperationException {
        List<IntelligentToolDefinition> definitions = contributedDefinitions();
        IntelligentToolContributor combinedContributor = () -> definitions;

        return new IntelligentToolCatalog(fixedContributorProvider(List.of(combinedContributor)));
    }

    /**
     * The definitions returned by the four real production {@link IntelligentToolContributor} beans, in contribution
     * order — the shared basis for {@link #buildCatalog()} and the per-definition {@code agentTypeKey()} assertion.
     */
    private static List<IntelligentToolDefinition> contributedDefinitions() throws ReflectiveOperationException {
        IntelligentToolContributor copilotContributor = invokeContributorFactory(
            COPILOT_CONTRIBUTOR_CLASS_NAME, COPILOT_CONTRIBUTOR_METHOD_NAME,
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatClientFactoryProvider(), chatModelResolverProvider());

        IntelligentToolContributor automationContributor = invokeContributorFactory(
            AUTOMATION_CONTRIBUTOR_CLASS_NAME, AUTOMATION_CONTRIBUTOR_METHOD_NAME,
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatClientFactoryProvider(), chatClientFactoryProvider(),
            chatModelResolverProvider());

        IntelligentToolContributor embeddedContributor = invokeContributorFactory(
            EMBEDDED_CONTRIBUTOR_CLASS_NAME, EMBEDDED_CONTRIBUTOR_METHOD_NAME, chatClientFactoryProvider(),
            chatModelResolverProvider());

        IntelligentToolContributor automationToolContributor = invokeContributorFactory(
            AUTOMATION_TOOL_CONTRIBUTOR_CLASS_NAME, AUTOMATION_TOOL_CONTRIBUTOR_METHOD_NAME,
            chatClientFactoryProvider(), chatModelResolverProvider());

        List<IntelligentToolDefinition> definitions = new ArrayList<>();

        definitions.addAll(copilotContributor.getIntelligentToolDefinitions());
        definitions.addAll(automationContributor.getIntelligentToolDefinitions());
        definitions.addAll(embeddedContributor.getIntelligentToolDefinitions());
        definitions.addAll(automationToolContributor.getIntelligentToolDefinitions());

        return definitions;
    }

    private static IntelligentToolContributor invokeContributorFactory(
        String contributorConfigurationClassName, String factoryMethodName, Object... providerArguments)
        throws ReflectiveOperationException {

        Class<?> contributorConfigurationClass = Class.forName(contributorConfigurationClassName);

        Constructor<?> constructor = contributorConfigurationClass.getDeclaredConstructor();

        constructor.setAccessible(true);

        Object contributorConfiguration = constructor.newInstance();

        Method factoryMethod = declaredFactoryMethod(contributorConfigurationClassName, factoryMethodName);

        return (IntelligentToolContributor) factoryMethod.invoke(contributorConfiguration, providerArguments);
    }

    private static Method declaredFactoryMethod(String className, String methodName)
        throws ReflectiveOperationException {

        Class<?> contributorConfigurationClass = Class.forName(className);

        Method factoryMethod = Arrays.stream(contributorConfigurationClass.getDeclaredMethods())
            .filter(method -> method.getName()
                .equals(methodName))
            .findFirst()
            .orElseThrow(() -> new NoSuchMethodException(methodName + " on " + className));

        factoryMethod.setAccessible(true);

        return factoryMethod;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolChatClientFactory> chatClientFactoryProvider() {
        ObjectProvider<IntelligentToolChatClientFactory> chatClientFactoryProvider = mock(ObjectProvider.class);

        when(chatClientFactoryProvider.getIfAvailable()).thenReturn(modelAwareChatClientFactory());

        return chatClientFactoryProvider;
    }

    /**
     * A fake {@link IntelligentToolChatClientFactory} — rather than a plain Mockito mock, whose unstubbed
     * {@code get(ChatModel)} would return {@code null} for every argument — that answers a distinct {@link ChatClient}
     * for {@code get(null)} versus {@code get(chatModel)}. Every call captures its own pair of clients, so each of the
     * seventeen provider instances built across the four contributors is independently distinguishable, which is what
     * lets {@code testEveryContributedDefinitionChatClientFactoryHonoursTheCallerPickedChatModel} detect a definition
     * that discards the caller-picked {@link ChatModel} instead of rebuilding its client over it.
     */
    private static IntelligentToolChatClientFactory modelAwareChatClientFactory() {
        ChatClient defaultChatClient = mock(ChatClient.class);
        ChatClient chatModelChatClient = mock(ChatClient.class);

        return chatModel -> chatModel == null ? defaultChatClient : chatModelChatClient;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider() {
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider = mock(ObjectProvider.class);

        when(chatModelResolverProvider.getIfAvailable()).thenReturn(mock(SubAgentChatModelResolver.class));

        return chatModelResolverProvider;
    }

    private static boolean isSubAgentChatModelResolverProvider(Parameter parameter) {
        if (!(parameter.getParameterizedType() instanceof ParameterizedType parameterizedType)) {
            return false;
        }

        return parameterizedType.getActualTypeArguments()[0] == SubAgentChatModelResolver.class;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedContributorProvider(
        List<IntelligentToolContributor> contributors) {

        ObjectProvider<IntelligentToolContributor> contributorProvider = mock(ObjectProvider.class);

        when(contributorProvider.orderedStream()).thenReturn(contributors.stream());

        return contributorProvider;
    }
}
